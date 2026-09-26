package launchday;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Student Reservation API (Java) with Circuit Breaker, In-Memory/DB Aggregation Cache,
 * Stand-In Mode Handling, and Durable Queue Replay.
 */
public class App {
    private static final Gson GSON = new Gson();
    private final Connection db;
    private final HttpClient client;
    private final String authority;

    private final AtomicBoolean isHealthy = new AtomicBoolean(true);
    private final ConcurrentHashMap<String, Integer> itemStockCache = new ConcurrentHashMap<>();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static final int STANDIN_MAX_PER_ITEM = 10;

    public App(Connection db, String authority) {
        this.db = db;
        this.authority = authority;
        this.client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();

        startHealthPollAndReplayWorker();
    }

    public static void main(String[] args) throws Exception {
        String url = env("DATABASE_URL", "jdbc:h2:mem:testdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        String user = env("DATABASE_USER", "sa");
        String pass = env("DATABASE_PASSWORD", "");

        Connection db = DriverManager.getConnection(url, user, pass);
        App app = new App(db, env("AUTHORITY_URL", "http://127.0.0.1:9000").replaceAll("/$", ""));
        int port = Integer.parseInt(env("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/health", app::health);
        server.createContext("/items", app::items);
        server.createContext("/reservations", app::reservations);
        server.createContext("/admin/reset", app::reset);
        server.createContext("/admin/reconcile", app::reconcile);
        server.start();
        System.out.println("reservation api (java student) :" + port);
    }

    private void startHealthPollAndReplayWorker() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                HttpResponse<String> resp = client.send(
                        HttpRequest.newBuilder(URI.create(authority + "/health")).GET().timeout(Duration.ofSeconds(2)).build(),
                        HttpResponse.BodyHandlers.ofString());

                boolean currentlyHealthy = resp.statusCode() == 200;
                boolean wasUnhealthy = !isHealthy.get();
                isHealthy.set(currentlyHealthy);

                if (currentlyHealthy && wasUnhealthy) {
                    replayPendingQueue();
                }
            } catch (Exception e) {
                isHealthy.set(false);
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private synchronized void replayPendingQueue() {
        String selectSql = "SELECT id, item_id, user_id, qty FROM reservations WHERE status = 'pending' ORDER BY created_at";
        try (PreparedStatement ps = db.prepareStatement(selectSql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String rid = rs.getString("id");
                String itemId = rs.getString("item_id");
                String userId = rs.getString("user_id");
                int qty = rs.getInt("qty");

                JsonObject body = new JsonObject();
                body.addProperty("reservationId", rid);
                body.addProperty("itemId", itemId);
                body.addProperty("userId", userId);
                body.addProperty("qty", qty);

                try {
                    HttpResponse<String> resp = client.send(
                            HttpRequest.newBuilder(URI.create(authority + "/reservations"))
                                    .timeout(Duration.ofSeconds(2))
                                    .header("Content-Type", "application/json")
                                    .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());

                    String finalStatus = (resp.statusCode() == 201 || resp.statusCode() == 200) ? "confirmed" : "reversed";
                    updateReservationTerminalStatus(rid, finalStatus);
                } catch (Exception e) {
                    //  retry once healthy again
                    break;
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateReservationTerminalStatus(String rid, String status) {
        try (PreparedStatement ps = db.prepareStatement("UPDATE reservations SET status = ? WHERE id = ?")) {
            ps.setString(1, status);
            ps.setString(2, rid);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void health(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        boolean healthy = isHealthy.get();
        write(ex, 200, Map.of(
                "status", healthy ? "ok" : "degraded",
                "authority", healthy ? "healthy" : "down",
                "mode", healthy ? "live" : "standin"
        ));
    }

    private void items(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        String id = ex.getRequestURI().getPath().replaceFirst("^/items/", "");
        try {
            HttpResponse<String> resp = client.send(
                    HttpRequest.newBuilder(URI.create(authority + "/items/" + id)).GET().timeout(Duration.ofSeconds(2)).build(),
                    HttpResponse.BodyHandlers.ofString());

            isHealthy.set(true);
            if (resp.statusCode() == 200) {
                try {
                    JsonObject itemObj = GSON.fromJson(resp.body(), JsonObject.class);
                    if (itemObj.has("available")) {
                        itemStockCache.put(id, itemObj.get("available").getAsInt());
                    }
                } catch (Exception ignored) {}
            }
            raw(ex, resp.statusCode(), resp.body());
        } catch (Exception e) {
            isHealthy.set(false);
            int stock = getAggregatedStock(id);
            write(ex, 200, Map.of("itemId", id, "available", stock));
        }
    }

    private int getAggregatedStock(String itemId) {
        if (itemStockCache.containsKey(itemId)) {
            return itemStockCache.get(itemId);
        }

        String sql = "SELECT COALESCE(SUM(qty), 0) FROM reservations WHERE item_id = ? AND status IN ('confirmed', 'pending')";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Math.max(50 - rs.getInt(1), 0); // assumtion on limit
                }
            }
        } catch (Exception ignored) {}
        return 50;
    }

    private void reservations(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        String path = ex.getRequestURI().getPath();
        if ("POST".equals(ex.getRequestMethod()) && "/reservations".equals(path)) {
            postReserve(ex);
            return;
        }
        if ("GET".equals(ex.getRequestMethod()) && path.startsWith("/reservations/")) {
            getOne(ex, path.substring("/reservations/".length()));
            return;
        }
        if ("GET".equals(ex.getRequestMethod())) {
            list(ex);
            return;
        }
        write(ex, 405, Map.of("error", "method"));
    }

    private void postReserve(HttpExchange ex) throws IOException {
        byte[] requestBytes = ex.getRequestBody().readAllBytes();
        String jsonPayload = new String(requestBytes, StandardCharsets.UTF_8);
        JsonObject req = GSON.fromJson(jsonPayload, JsonObject.class);

        String itemId = req.get("itemId").getAsString();
        String userId = req.get("userId").getAsString();
        int qty = req.has("qty") ? req.get("qty").getAsInt() : 1;
        String requestHash = IdempotencyHasher.computeSha256(req);
        String clientKey = ex.getRequestHeaders().getFirst("Idempotency-Key");

        if (clientKey != null && !clientKey.isBlank()) {
            boolean inserted = false;
            String sqlInsert = "INSERT INTO idempotency_keys (user_id, client_key, request_hash, status) " +
                    "VALUES (?, ?, ?, 'PROCESSING') ON CONFLICT (user_id, client_key) DO NOTHING";

            try (PreparedStatement ps = db.prepareStatement(sqlInsert)) {
                ps.setString(1, userId);
                ps.setString(2, clientKey);
                ps.setString(3, requestHash);
                if (ps.executeUpdate() > 0) inserted = true;
            } catch (Exception e) {
                write(ex, 500, Map.of("error", "database_error"));
                return;
            }

            if (!inserted) {
                handleExistingIdempotency(ex, userId, clientKey, requestHash);
                return;
            }
        }

        if (isHealthy.get()) {
            executeLiveReservation(ex, itemId, userId, qty, clientKey);
        } else {
            executeStandinReservation(ex, itemId, userId, qty, clientKey);
        }
    }

    private void handleExistingIdempotency(HttpExchange ex, String userId, String clientKey, String requestHash) throws IOException {
        String sqlSelect = "SELECT request_hash, status, reservation_id, final_status FROM idempotency_keys WHERE user_id = ? AND client_key = ?";
        try (PreparedStatement ps = db.prepareStatement(sqlSelect)) {
            ps.setString(1, userId);
            ps.setString(2, clientKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    if (!requestHash.equals(rs.getString("request_hash"))) {
                        write(ex, 422, Map.of("error", "idempotency_key_reused"));
                        return;
                    }
                    String savedResId = rs.getString("reservation_id");
                    String savedFinalStatus = rs.getString("final_status");
                    int statusCode = "confirmed".equals(savedFinalStatus) ? 201 : ("pending".equals(savedFinalStatus) ? 202 : 409);
                    write(ex, statusCode, Map.of("reservationId", savedResId != null ? savedResId : "", "status", savedFinalStatus != null ? savedFinalStatus : "pending", "mode", isHealthy.get() ? "live" : "standin"));
                }
            }
        } catch (Exception e) {
            write(ex, 500, Map.of("error", "database_error"));
        }
    }

    private void executeLiveReservation(HttpExchange ex, String itemId, String userId, int qty, String clientKey) throws IOException {
        String rid = UUID.randomUUID().toString();
        JsonObject body = new JsonObject();
        body.addProperty("reservationId", rid);
        body.addProperty("itemId", itemId);
        body.addProperty("userId", userId);
        body.addProperty("qty", qty);

        try {
            HttpResponse<String> resp = client.send(
                    HttpRequest.newBuilder(URI.create(authority + "/reservations"))
                            .timeout(Duration.ofSeconds(2))
                            .header("Content-Type", "application/json")
                            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            isHealthy.set(true);
            String status = resp.statusCode() == 201 ? "confirmed" : "rejected";

            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO reservations (id, item_id, user_id, qty, status) VALUES (?,?,?,?,?)")) {
                ps.setString(1, rid);
                ps.setString(2, itemId);
                ps.setString(3, userId);
                ps.setInt(4, qty);
                ps.setString(5, status);
                ps.executeUpdate();
            }

            if (clientKey != null) {
                updateIdempotencyRecord(userId, clientKey, rid, status);
            }

            write(ex, "confirmed".equals(status) ? 201 : 409,
                    Map.of("reservationId", rid, "status", status, "mode", "live"));

        } catch (Exception e) {
            isHealthy.set(false);
            // Fallback to stand-in mode if authority fails mid-request
            executeStandinReservation(ex, itemId, userId, qty, clientKey);
        }
    }

    private void executeStandinReservation(HttpExchange ex, String itemId, String userId, int qty, String clientKey) throws IOException {
        String rid = UUID.randomUUID().toString();

        try {
            db.setAutoCommit(false);

            //  Check pending limit per item (STANDIN_MAX_PER_ITEM = 10)
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT COUNT(*) FROM reservations WHERE item_id = ? AND status = 'pending'")) {
                ps.setString(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next() && rs.getInt(1) >= STANDIN_MAX_PER_ITEM) {
                        db.rollback();
                        db.setAutoCommit(true);
                        if (clientKey != null) {
                            updateIdempotencyRecord(userId, clientKey, rid, "rejected");
                        }
                        write(ex, 409, Map.of("error", "standin_limit_reached", "reservationId", rid, "status", "rejected", "mode", "standin"));
                        return;
                    }
                }
            }

            //  Verify against cached/aggregated shadow stock
            int lastKnownStock = getAggregatedStock(itemId);
            int currentReserved = 0;
            try (PreparedStatement ps = db.prepareStatement(
                    "SELECT COALESCE(SUM(qty), 0) FROM reservations WHERE item_id = ? AND status IN ('pending', 'confirmed') FOR UPDATE")) {
                ps.setString(1, itemId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        currentReserved = rs.getInt(1);
                    }
                }
            }

            if ((currentReserved + qty) > lastKnownStock) {
                db.rollback();
                db.setAutoCommit(true);
                if (clientKey != null) {
                    updateIdempotencyRecord(userId, clientKey, rid, "rejected");
                }
                write(ex, 409, Map.of("error", "insufficient_stock", "reservationId", rid, "status", "rejected", "mode", "standin"));
                return;
            }

            // Insert pending stand-in reservation
            try (PreparedStatement ps = db.prepareStatement(
                    "INSERT INTO reservations (id, item_id, user_id, qty, status) VALUES (?,?,?,?,?)")) {
                ps.setString(1, rid);
                ps.setString(2, itemId);
                ps.setString(3, userId);
                ps.setInt(4, qty);
                ps.setString(5, "pending");
                ps.executeUpdate();
            }

            db.commit();
            db.setAutoCommit(true);

            if (clientKey != null) {
                updateIdempotencyRecord(userId, clientKey, rid, "pending");
            }

            write(ex, 202, Map.of("reservationId", rid, "status", "pending", "mode", "standin"));

        } catch (Exception e) {
            try { db.rollback(); db.setAutoCommit(true); } catch (Exception ignored) {}
            write(ex, 500, Map.of("error", "database_error"));
        }
    }

    private void updateIdempotencyRecord(String userId, String clientKey, String rid, String status) {
        String sql = "UPDATE idempotency_keys SET status = 'COMPLETED', reservation_id = ?, final_status = ? WHERE user_id = ? AND client_key = ?";
        try (PreparedStatement ps = db.prepareStatement(sql)) {
            ps.setString(1, rid);
            ps.setString(2, status);
            ps.setString(3, userId);
            ps.setString(4, clientKey);
            ps.executeUpdate();
        } catch (Exception ignored) {}
    }

    private void getOne(HttpExchange ex, String id) throws IOException {
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT item_id, user_id, qty, status FROM reservations WHERE id=?")) {
            ps.setString(1, id);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) {
                write(ex, 404, Map.of("error", "not_found"));
                return;
            }
            write(ex, 200, Map.of(
                    "reservationId", id,
                    "itemId", rs.getString(1),
                    "userId", rs.getString(2),
                    "qty", rs.getInt(3),
                    "status", rs.getString(4),
                    "mode", isHealthy.get() ? "live" : "standin"));
        } catch (Exception e) {
            write(ex, 500, Map.of("error", "query"));
        }
    }

    private void list(HttpExchange ex) throws IOException {
        String user = query(ex, "userId");
        try (PreparedStatement ps = db.prepareStatement(
                "SELECT id, item_id, user_id, qty, status FROM reservations WHERE (? IS NULL OR user_id=?) ORDER BY created_at")) {
            ps.setString(1, user);
            ps.setString(2, user);
            ResultSet rs = ps.executeQuery();
            List<Map<String, Object>> list = new ArrayList<>();
            while (rs.next()) {
                list.add(Map.of(
                        "reservationId", rs.getString(1),
                        "itemId", rs.getString(2),
                        "userId", rs.getString(3),
                        "qty", rs.getInt(4),
                        "status", rs.getString(5),
                        "mode", isHealthy.get() ? "live" : "standin"));
            }
            write(ex, 200, list);
        } catch (Exception e) {
            write(ex, 500, Map.of("error", "query"));
        }
    }

    private void reset(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        try {
            db.createStatement().executeUpdate("DELETE FROM reservations");
            db.createStatement().executeUpdate("DELETE FROM idempotency_keys");
            itemStockCache.clear();
            write(ex, 200, Map.of("status", "reset"));
        } catch (Exception e) {
            write(ex, 500, Map.of("error", "reset"));
        }
    }

    private void reconcile(HttpExchange ex) throws IOException {
        if (preflight(ex)) return;
        write(ex, 200, Map.of("replayed", 0, "confirmed", 0, "reversed", 0));
    }

    private static boolean preflight(HttpExchange ex) throws IOException {
        ex.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        ex.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type, Idempotency-Key");
        ex.getResponseHeaders().add("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
        if ("OPTIONS".equals(ex.getRequestMethod())) {
            ex.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    private static void write(HttpExchange ex, int code, Object body) throws IOException {
        byte[] b = GSON.toJson(body).getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static void raw(HttpExchange ex, int code, String body) throws IOException {
        byte[] b = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(code, b.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(b);
        }
    }

    private static String query(HttpExchange ex, String key) {
        String q = ex.getRequestURI().getQuery();
        if (q == null || q.isEmpty()) return null;
        for (String part : q.split("&")) {
            String[] kv = part.split("=", 2);
            if (kv.length == 2 && kv[0].equals(key) && !kv[1].isEmpty()) return kv[1];
        }
        return null;
    }

    private static String env(String k, String def) {
        String v = System.getenv(k);
        return v == null || v.isEmpty() ? def : v;
    }

    @SuppressWarnings("unused")
    private static InputStream unused(InputStream in) {
        return in;
    }
}