package seller;

import java.util.Map;

public class ProductInventory {
    private final Map<String, Integer> stock;

    public ProductInventory(Map<String, Integer> initial) {
        this.stock = initial;
    }

    public synchronized boolean reserve(String product, int qty) {
        int available = stock.getOrDefault(product, 0);
        if (available >= qty) {
            stock.put(product, available - qty);
            return true;
        } else {
            return false;
        }
    }

    public synchronized void commit(String product, int qty) {
        // nothing needed here since reserve already reduced
    }

    public synchronized void rollback(String product, int qty) {
        stock.put(product, stock.getOrDefault(product, 0) + qty);
    }
}
