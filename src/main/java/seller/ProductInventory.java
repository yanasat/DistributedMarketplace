package seller;

import java.util.HashMap;
import java.util.Map;

public class ProductInventory {
    private final Map<String, Integer> stock;
    private final Map<String, Integer> reservations;

    public ProductInventory(Map<String, Integer> initialStock) {
        this.stock = new HashMap<>(initialStock);
        this.reservations = new HashMap<>();
    }

    public synchronized boolean reserve(String productId, int quantity) {
        int available = stock.getOrDefault(productId, 0);
        if (available >= quantity) {
            stock.put(productId, available - quantity);
            reservations.put(productId, reservations.getOrDefault(productId, 0) + quantity);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void commit(String productId, int quantity) {
        reservations.put(productId, reservations.getOrDefault(productId, 0) - quantity);
    }

    public synchronized void rollback(String productId, int quantity) {
        int reserved = reservations.getOrDefault(productId, 0);
        if (reserved >= quantity) {
            reservations.put(productId, reserved - quantity);
            stock.put(productId, stock.getOrDefault(productId, 0) + quantity);
        }
    }
}
