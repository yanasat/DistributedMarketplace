package model;

import java.util.HashMap;
import java.util.Map;

public class Order {
    private final String id;
    private final String product;
    private final int quantity;
    private final String marketplaceId; // NEU: Marketplace-Identifikation
    private final Map<String, Status> sellerStatus = new HashMap<>();

    public enum Status {
        CONFIRMED, REJECTED, PENDING
    }

    public Order(String product, int quantity, String marketplaceId) {
        // Eindeutige ID mit Marketplace-Prefix
        this.id = marketplaceId + "-" + System.currentTimeMillis() + "-" + 
                 java.util.UUID.randomUUID().toString().substring(0, 8);
        this.product = product;
        this.quantity = quantity;
        this.marketplaceId = marketplaceId;
    }

    // Backward compatibility
    public Order(String product, int quantity) {
        this(product, quantity, "MP-DEFAULT");
    }

    public String getId() { return id; }
    public String getProduct() { return product; }
    public int getQuantity() { return quantity; }
    public String getMarketplaceId() { return marketplaceId; }

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

    @Override
    public String toString() {
        return String.format("Order{id='%s', marketplace='%s', product='%s', qty=%d}", 
                           id, marketplaceId, product, quantity);
    }
}