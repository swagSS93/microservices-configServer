/*
package practiceTest;

import java.util.HashMap;
import java.util.Map;

public class LRUCacheImp {
    public static void main(String args[]){
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

class Node{
    int key, value;
    Node next, prev;
    Node(int key, int value){
        this.key = key;
        this.value = value;
        next = null;
        prev = null;
    }
}

class LRUCache{
    Node head, tail;
    int capacity;
    Map<Integer, Node> cacheMap;

    LRUCache(int capacity){
        cacheMap = new HashMap<>();
        this.capacity = capacity;
        this.head = new Node(-1,-1);
        this.tail = new Node(-1,-1);
        this.head.next = this.tail;
        this.tail.prev = this.head;
    }

    void add(Node node){
        Node nextNode = head.next;
        node.next = nextNode;
        nextNode.prev = node;
        node.prev = head;
        head.next = node;
    }

    void delete(Node node){
        Node prevNode = node.prev;
        prevNode.next = node.next;
        node.next.prev = prevNode;
    }

    int get(int key){
        if(!cacheMap.containsKey(key))
            return -1;
            Node node = cacheMap.get(key);
            delete(node);
            add(node);
            return node.value;
    }
    void put(int key, int value){
        if (cacheMap.containsKey(key)) {
            Node node = cacheMap.get(key);
            delete(node);
        }

        Node newNode = new Node(key,value);
        cacheMap.put(key, newNode);
        add(newNode);

        if(cacheMap.size() > capacity){
            Node nodeTobeDeleted = cacheMap.get(key);
            delete(nodeTobeDeleted);
            cacheMap.remove(nodeTobeDeleted.key);
        }
    }
}
*/
