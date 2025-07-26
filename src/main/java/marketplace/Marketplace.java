package marketplace;

import messaging.MessageUtils;
import org.zeromq.ZMQ;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import model.Order;
import model.Order.Status;

// Marketplace class manages placing orders to multiple sellers and handles rollback if needed
public class Marketplace {
    private static final Logger LOGGER = Logger.getLogger(Marketplace.class.getName());
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
            try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                socket.send("ORDER:" + product);
                String response = socket.recvStr();
                if (LOGGER.isLoggable(Level.INFO)) {
                    LOGGER.info(String.format("Response from %s: %s", endpoint, response));
                }

                // Map response to order status
                Status status = Status.PENDING; // Default status
                switch (response) {
                    case "CONFIRMED":
                        status = Status.CONFIRMED;
                        break;
                    case "REJECTED":
                        status = Status.REJECTED;
                        break;
                    default:
                        status = Status.PENDING;
                        break;
                }
                order.setStatus(endpoint, status);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, String.format("Error communicating with seller: %s", endpoint), e);
            }
        }

        // If all sellers confirm, order is successful; otherwise, rollback
        if (order.isFullyConfirmed()) {
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("Order successful for product: %s", product));
            }
        } else {
            LOGGER.warning("Order failed. Starting rollback...");
            rollback(order);
        }
    }

    // Rollback confirmed orders if not all sellers confirmed
    private void rollback(Order order) {
        for (String endpoint : order.getSellerStatus().keySet()) {
            if (order.getStatus(endpoint) == Status.CONFIRMED) {
                try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                    socket.send("CANCEL:" + order.getProduct());
                    String response = socket.recvStr();
                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.info(String.format("Rollback response from %s: %s", endpoint, response));
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, String.format("Error during rollback with seller: %s", endpoint), e);
                }
            }
        }
    }
}