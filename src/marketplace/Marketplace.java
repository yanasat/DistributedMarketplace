package marketplace;

import messaging.MessageUtils;
import org.zeromq.ZMQ;
import java.util.List;
import model.Order;
import model.Order.Status;
import java.util.HashMap;

// Marketplace class manages placing orders to multiple sellers and handles rollback if needed
public class Marketplace {
    private final List<String> sellerEndpoints;

    // Initialize with a list of seller endpoints
    public Marketplace(List<String> sellerEndpoints) {
        this.sellerEndpoints = sellerEndpoints;
    }

    // Place an order for a product to all sellers
    public void placeOrder(String product) {
        Order order = new Order(product);

        // Send order to each seller and collect their responses
        for (String endpoint : sellerEndpoints) {
            ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint);
            socket.send("ORDER:" + product);
            String response = socket.recvStr();
            System.out.println("Response from " + endpoint + ": " + response);

            // Map response to order status
            Status status = switch (response) {
                case "CONFIRMED" -> Status.CONFIRMED;
                case "REJECTED" -> Status.REJECTED;
                default -> Status.PENDING;
            };
            order.setStatus(endpoint, status);
            socket.close();
        }

        // If all sellers confirm, order is successful; otherwise, rollback
        if (order.isFullyConfirmed()) {
            System.out.println("Order successful for product: " + product);
        } else {
            System.out.println("Order failed. Starting rollback...");
            rollback(order);
        }
    }

    // Rollback confirmed orders if not all sellers confirmed
    private void rollback(Order order) {
        for (String endpoint : order.getSellerStatus().keySet()) {
            if (order.getStatus(endpoint) == Status.CONFIRMED) {
                ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint);
                socket.send("CANCEL:" + order.getProduct());
                String response = socket.recvStr();
                System.out.println("Rollback response from " + endpoint + ": " + response);
                socket.close();
            }
        }
    }
}
