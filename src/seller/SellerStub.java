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
                boolean hasProduct = rand.nextBoolean(); // 50% chance to have the product
                String response = hasProduct ? "CONFIRMED" : "REJECTED";
                socket.send(response);
            } else if (msg.startsWith("CANCEL:")) {
                // Handle order cancellation
                System.out.println("Order cancelled!");
                socket.send("CANCELLED");
            }
        }
    }
}
