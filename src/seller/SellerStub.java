package seller;

import messaging.MessageUtils;
import org.zeromq.ZMQ;
import java.util.Random;

// SellerStub simulates a seller that can confirm or reject orders randomly
public class SellerStub {

    // Start the seller at the given endpoint
    public static void start(String endpoint) {
        ZMQ.Socket socket = MessageUtils.createSocket("REP", true, endpoint);
        System.out.println("Seller online at " + endpoint);
        Random rand = new Random();

        // Main loop: handle incoming messages
        while (!Thread.currentThread().isInterrupted()) {
            String msg = socket.recvStr();
            System.out.println("Received: " + msg);

            // Handle order requests
            if (msg.startsWith("ORDER:")) {
                String product = msg.substring(6); // Extract product name
                boolean hasProduct = rand.nextBoolean(); // 50% chance to have the product
                String response = hasProduct ? "CONFIRMED" : "REJECTED";
                
                if (hasProduct) {
                    System.out.println("[CONFIRM] Accepting order for: " + product);
                } else {
                    System.out.println("[REJECT] Declining order for: " + product);
                }
                
                socket.send(response);
            } else if (msg.startsWith("CANCEL:")) {
                String product = msg.substring(7); // Extract product name
                System.out.println("[CANCEL] Order cancelled for: " + product);
                socket.send("CANCELLED");
            } else if (msg.equals("HEALTH_CHECK")) {
                // Respond to health check requests
                socket.send("HEALTHY");
            }
        }
    }
}
