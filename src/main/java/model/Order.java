package model;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private final String id;
    private final String product;
    private final int quantity;   // ✅ NEU: Menge
    private final Map<String, Status> sellerStatus = new HashMap<>();

    public enum Status {
        CONFIRMED, REJECTED, PENDING
    }

    public Order(String product, int quantity) {
        this.id = java.util.UUID.randomUUID().toString();
        this.product = product;
        this.quantity = quantity;
    }

    public String getId() { return id; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }

    public void setStatus(String endpoint, Status status) {
        sellerStatus.put(endpoint, status);
    }

    public Status getStatus(String endpoint) {
        return sellerStatus.getOrDefault(endpoint, Status.PENDING);
    }

    public boolean isFullyConfirmed() {
        return sellerStatus.values().stream().allMatch(s -> s == Status.CONFIRMED);
    }

    public Map<String, Status> getSellerStatus() {
        return sellerStatus;
    }
}
