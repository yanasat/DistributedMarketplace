package model;

import java.util.HashMap;
import java.util.Map;

// Order class represents an order for a product and tracks status per seller
public class Order {
    // Possible statuses for an order
    public enum Status { PENDING, CONFIRMED, REJECTED, CANCELLED }

    private String product;
    // Map from seller endpoint to order status
    private Map<String, Status> sellerStatus = new HashMap<>();

    // Create a new order for a product
    public Order(String product) {
        this.product = product;
    }

    // Set the status for a seller
    public void setStatus(String seller, Status status) {
        sellerStatus.put(seller, status);
    }

    // Get the status for a seller (default: PENDING)
    public Status getStatus(String seller) {
        return sellerStatus.getOrDefault(seller, Status.PENDING);
    }

    // Check if all sellers confirmed the order
    public boolean isFullyConfirmed() {
        return sellerStatus.values().stream().allMatch(s -> s == Status.CONFIRMED);
    }

    // Check if any seller rejected the order
    public boolean isRejected() {
        return sellerStatus.values().contains(Status.REJECTED);
    }

    // Get the product name
    public String getProduct() {
        return product;
    }

    // Get the map of seller statuses
    public Map<String, Status> getSellerStatus() {
        return sellerStatus;
    }
}
