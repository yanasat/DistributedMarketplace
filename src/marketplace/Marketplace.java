package marketplace;

import java.util.List;

import org.zeromq.ZMQ;

import messaging.MessageUtils;
import model.Order;
import model.Order.Status;

public class Marketplace {
    private final List<String> sellerEndpoints;
    private volatile boolean running = true;

    public Marketplace(List<String> sellerEndpoints) {
        this.sellerEndpoints = sellerEndpoints;
    }

    public void placeOrder(String product) {
        Order order = new Order(product);

        // Phase 1: RESERVE bei allen Sellern
        for (String endpoint : sellerEndpoints) {
            try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                socket.send("RESERVE:" + order.getId() + ":" + product + ":1");
                String response = socket.recvStr(ZMQ.DONTWAIT);

                if (response == null) {
                    System.out.println("No response from " + endpoint + " — treating as failure.");
                    order.setStatus(endpoint, Status.REJECTED);
                    continue;
                }

                System.out.println("Response from " + endpoint + ": " + response);

                Status status;
                String respType = response.split(":")[0];
                switch (respType) {
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
                System.out.println("Error communicating with " + endpoint + ": " + e.getMessage());
                order.setStatus(endpoint, Status.REJECTED);
            }
        }

        // Phase 2: COMMIT oder CANCEL
        if (order.isFullyConfirmed()) {
            System.out.println("All sellers confirmed — sending COMMIT...");
            for (String endpoint : sellerEndpoints) {
                try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                    socket.send("COMMIT:" + order.getId() + ":" + product + ":1");
                    String response = socket.recvStr(ZMQ.DONTWAIT);
                    System.out.println("Commit response from " + endpoint + ": " + response);
                } catch (Exception e) {
                    System.out.println("Error during commit with " + endpoint + ": " + e.getMessage());
                }
            }
            System.out.println("Order successful for product: " + product);
        } else {
            System.out.println("Reservation failed — rolling back...");
            rollback(order);
        }
    }

    private void rollback(Order order) {
        for (String endpoint : order.getSellerStatus().keySet()) {
            if (order.getStatus(endpoint) == Status.CONFIRMED) {
                try (ZMQ.Socket socket = MessageUtils.createSocket("REQ", false, endpoint)) {
                    socket.send("CANCEL:" + order.getId() + ":" + order.getProduct() + ":1");
                    String response = socket.recvStr();
                    System.out.println("Rollback response from " + endpoint + ": " + response);
                }
            }
        }
    }

    public void stop() {
        running = false;
        System.out.println("Marketplace stopped.");
    }
}
