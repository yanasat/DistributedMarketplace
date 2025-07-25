// MultiProductOrder.java - Enhanced order model for multi-product orders
// Part of Yana's SAGA implementation

package model;

import java.util.HashMap;
import java.util.Map;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class MultiProductOrder {
    private final String orderId;
    private final Map<String, Integer> products = new HashMap<>(); // Product -> Quantity
    private final LocalDateTime orderTime = LocalDateTime.now();
    private OrderStatus status = OrderStatus.PENDING;
    private String failureReason;
    
    public enum OrderStatus {
        PENDING,     // Order created, not yet processed
        RESERVING,   // Phase 1: Reserving products
        RESERVED,    // Phase 1: All products reserved
        COMMITTING,  // Phase 2: Committing reservations
        COMPLETED,   // Phase 2: All committed successfully
        FAILED,      // Order failed at some point
        ROLLED_BACK  // Order was rolled back
    }
    
    public MultiProductOrder(String orderId) {
        this.orderId = orderId;
        System.out.println("📦 Created order: " + orderId + " at " + 
            orderTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
    }
    
    // Add product to order
    public void addProduct(String product, int quantity) {
        products.put(product, quantity);
        System.out.println("  ➕ Added: " + quantity + "x " + product);
    }
    
    // Remove product from order
    public void removeProduct(String product) {
        Integer removed = products.remove(product);
        if (removed != null) {
            System.out.println("  ➖ Removed: " + removed + "x " + product);
        }
    }
    
    // Update product quantity
    public void updateProductQuantity(String product, int newQuantity) {
        if (products.containsKey(product)) {
            int oldQuantity = products.get(product);
            products.put(product, newQuantity);
            System.out.println("  📝 Updated " + product + ": " + oldQuantity + " → " + newQuantity);
        }
    }
    
    // Get total number of items
    public int getTotalItems() {
        return products.values().stream().mapToInt(Integer::intValue).sum();
    }
    
    // Get total number of different products
    public int getProductCount() {
        return products.size();
    }
    
    // Check if order contains a specific product
    public boolean containsProduct(String product) {
        return products.containsKey(product);
    }
    
    // Get quantity of a specific product
    public int getProductQuantity(String product) {
        return products.getOrDefault(product, 0);
    }
    
    // Calculate estimated processing complexity (for monitoring)
    public int getComplexityScore() {
        // Complexity based on number of products and total quantity
        return getProductCount() * 2 + getTotalItems();
    }
    
    // Pretty print order summary
    public void printOrderSummary() {
        System.out.println("📋 ORDER SUMMARY: " + orderId);
        System.out.println("   🕐 Created: " + orderTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")));
        System.out.println("   📊 Status: " + status);
        System.out.println("   📦 Products: " + getProductCount() + " types, " + getTotalItems() + " total items");
        
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            System.out.println("     • " + entry.getValue() + "x " + entry.getKey());
        }
        
        if (failureReason != null) {
            System.out.println("   ❌ Failure: " + failureReason);
        }
    }
    
    // Convert to string for easy transmission/logging
    public String toOrderString() {
        StringBuilder sb = new StringBuilder();
        sb.append(orderId).append("|");
        
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            sb.append(entry.getKey()).append(":").append(entry.getValue()).append(",");
        }
        
        // Remove trailing comma
        if (sb.length() > 0 && sb.charAt(sb.length() - 1) == ',') {
            sb.setLength(sb.length() - 1);
        }
        
        return sb.toString();
    }
    
    // Parse order from string (for network transmission)
    public static MultiProductOrder fromOrderString(String orderString) {
        String[] parts = orderString.split("\\|");
        if (parts.length != 2) {
            throw new IllegalArgumentException("Invalid order string format");
        }
        
        String orderId = parts[0];
        MultiProductOrder order = new MultiProductOrder(orderId);
        
        if (!parts[1].isEmpty()) {
            String[] products = parts[1].split(",");
            for (String product : products) {
                String[] productParts = product.split(":");
                if (productParts.length == 2) {
                    String productName = productParts[0];
                    int quantity = Integer.parseInt(productParts[1]);
                    order.addProduct(productName, quantity);
                }
            }
        }
        
        return order;
    }
    
    // Validate order (business rules)
    public boolean isValid() {
        // Order must have at least one product
        if (products.isEmpty()) {
            failureReason = "Order contains no products";
            return false;
        }
        
        // No product can have zero or negative quantity
        for (Map.Entry<String, Integer> entry : products.entrySet()) {
            if (entry.getValue() <= 0) {
                failureReason = "Invalid quantity for product " + entry.getKey() + ": " + entry.getValue();
                return false;
            }
        }
        
        // Order cannot be too large (business constraint)
        if (getTotalItems() > 50) {
            failureReason = "Order too large: " + getTotalItems() + " items (max 50)";
            return false;
        }
        
        return true;
    }
    
    // Getters and Setters
    public String getOrderId() { return orderId; }
    public Map<String, Integer> getProducts() { return new HashMap<>(products); }
    public LocalDateTime getOrderTime() { return orderTime; }
    public OrderStatus getStatus() { return status; }
    public String getFailureReason() { return failureReason; }
    
    public void setStatus(OrderStatus status) { 
        System.out.println("🔄 Order " + orderId + " status: " + this.status + " → " + status);
        this.status = status; 
    }
    
    public void setFailureReason(String failureReason) { 
        this.failureReason = failureReason;
        System.out.println("❌ Order " + orderId + " failed: " + failureReason);
    }
    
    // Equals and HashCode based on orderId
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        MultiProductOrder that = (MultiProductOrder) obj;
        return orderId.equals(that.orderId);
    }
    
    @Override
    public int hashCode() {
        return orderId.hashCode();
    }
    
    @Override
    public String toString() {
        return "MultiProductOrder{" +
                "orderId='" + orderId + '\'' +
                ", products=" + products +
                ", status=" + status +
                ", totalItems=" + getTotalItems() +
                '}';
    }
}