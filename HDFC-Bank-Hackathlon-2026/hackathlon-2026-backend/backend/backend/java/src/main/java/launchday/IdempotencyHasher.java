package launchday;

import com.google.gson.JsonObject;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

public class IdempotencyHasher {

    private static final HexFormat HEX_FORMATTER = HexFormat.of();

    public static String computeSha256(JsonObject req) {
        try {

            String itemId = req.has("itemId") ? req.get("itemId").getAsString() : "";
            String userId = req.has("userId") ? req.get("userId").getAsString() : "";
            int qty = req.has("qty") ? req.get("qty").getAsInt() : 1;

            String canonicalString = String.format("itemId:%s|qty:%d|userId:%s", itemId, qty, userId);

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(canonicalString.getBytes(StandardCharsets.UTF_8));

            return HEX_FORMATTER.formatHex(hashBytes);

        } catch (Exception ex) {
            throw new IllegalStateException("Failed to calculate payload signature", ex);
        }
    }
}

