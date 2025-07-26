package seller;

import java.util.Random;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Seller {
    private final SellerConfig config;
    private final ProductInventory inventory;
    private final Random rand;
    private volatile boolean running = true;

    public Seller(SellerConfig config) {
        this.config = config;
        this.inventory = new ProductInventory(config.products);
        this.rand = new Random();
    }

    public void start() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(ZMQ.REP);
            String endpoint = "tcp://*:" + config.port;
            socket.bind(endpoint);
            System.out.println("Seller running on " + endpoint);

            while (running && !Thread.currentThread().isInterrupted()) {
                String request = socket.recvStr();
                System.out.println("Received: " + request);

                simulateLatency();

                if (simulateCrash()) {
                    System.out.println("Simulated crash — no reply.");
                    continue;
                }

                if (simulateLostAck()) {
                    System.out.println("Simulated lost ack — no reply.");
                    continue;
                }

                String[] parts = request.split(":");
                String command = parts[0];
                String orderId = parts[1];
                String productId = parts[2];
                int quantity = Integer.parseInt(parts[3]);

                String reply;
                switch (command) {
                    case "RESERVE":
                        reply = inventory.reserve(productId, quantity) ? "CONFIRMED:" + orderId : "REJECTED:" + orderId;
                        break;
                    case "COMMIT":
                        inventory.commit(productId, quantity);
                        reply = "COMMITTED:" + orderId;
                        break;
                    case "CANCEL":
                        inventory.rollback(productId, quantity);
                        reply = "ROLLED_BACK:" + orderId;
                        break;
                    default:
                        reply = "UNKNOWN_COMMAND";
                }

                socket.send(reply);
            }
            socket.close();
        }
    }

    public void stop() {
        running = false;
    }

    private void simulateLatency() {
        try {
            int latency = Math.max(0, (int) (rand.nextGaussian() * config.avgLatencyMs));
            Thread.sleep(latency);
        } catch (InterruptedException ignored) {}
    }

    private boolean simulateCrash() {
        return rand.nextDouble() < config.crashProbability;
    }

    private boolean simulateLostAck() {
        return rand.nextDouble() < config.lostAckProbability;
    }
}
