import java.util.HashMap;
import java.util.Map;

class Node{
    int key, value;
    Node next, prev;
    Node(int key, int value){
        this.key = key;
        this.value = value;
    }

}

 class LRUCache {
    Node head, tail;
    Map<Integer, Node> cacheMap;
    int capacity;
    LRUCache(int capacity){
        this.capacity = capacity;
        cacheMap = new HashMap<>();
        this.head = new Node(-1,-1);
        this.tail = new Node(-1,-1);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

     int get(int key){
        if (!cacheMap.containsKey(key))
            return  -1;

        Node node = cacheMap.get(key);
        delete(node);
        add(node);

        return node.value;
    }

     void put(int key, int value){
        if(cacheMap.containsKey(key)){
            Node node = cacheMap.get(key);
            delete(node);
        }

        Node nodeTobeAdded = new Node(key, value);
        cacheMap.put(key, nodeTobeAdded);
        add(nodeTobeAdded);

        if(cacheMap.size() > capacity){
            Node nodeTobeDeleted = tail.prev;
            delete(nodeTobeDeleted);
            cacheMap.remove(nodeTobeDeleted.key);
        }
    }

    private void add(Node node){
        Node nextNode = head.next;
        node.next = nextNode;
        node.prev = head;
        nextNode.prev = node;
        head.next = node;
    }

    private void delete(Node node){
        Node prevNode = node.prev;
        prevNode.next = node.next;
        node.next.prev = prevNode;
    }

}

public class LRUCacheImpl{
    public static void main(String arg[]){
        LRUCache cache = new LRUCache(2);
        cache.put(1, 1);
        cache.put(2, 2);
        System.out.println(cache.get(1));
        cache.put(3, 3);
        System.out.println(cache.get(2));
        cache.put(4, 4);
        System.out.println(cache.get(1));
        System.out.println(cache.get(3));
        System.out.println(cache.get(4));
    }
}
