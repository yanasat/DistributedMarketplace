package seller;

import java.util.Random;

import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Seller {
    private final SellerConfig config;
    private final ProductInventory inventory;
    private final Random random = new Random();

    public Seller(SellerConfig config) {
        this.config = config;
        this.inventory = new ProductInventory(config.products);
    }

    public void start() {
        try (ZContext context = new ZContext()) {
            ZMQ.Socket socket = context.createSocket(ZMQ.REP);
            socket.bind("tcp://*:" + config.port);
            System.out.println("Seller listening on port " + config.port);

            while (!Thread.currentThread().isInterrupted()) {
                String msg = socket.recvStr();
                System.out.println("Received: " + msg);

                simulateLatency();
                if (simulateCrash()) {
                    System.out.println("Simulating crash: ignoring message.");
                    continue;
                }
                if (simulateLostAck()) {
                    System.out.println("Simulating lost ack: not replying.");
                    continue;
                }

                String[] parts = msg.split(":");
                String cmd = parts[0];
                String orderId = parts[1];
                String product = parts[2];
                int qty = Integer.parseInt(parts[3]);

                String reply;
                switch (cmd) {
                    case "RESERVE":
                        reply = inventory.reserve(product, qty) ? "CONFIRMED:" + orderId : "REJECTED:" + orderId;
                        break;
                    case "COMMIT":
                        inventory.commit(product, qty);
                        reply = "COMMITTED:" + orderId;
                        break;
                    case "CANCEL":
                        inventory.rollback(product, qty);
                        reply = "ROLLED_BACK:" + orderId;
                        break;
                    default:
                        reply = "UNKNOWN:" + orderId;
                }
                socket.send(reply);
            }
        }
    }

    private void simulateLatency() {
        try { Thread.sleep(Math.max(0, (long) (random.nextGaussian() * config.avgLatencyMs))); } catch (InterruptedException ignored) {}
    }

    private boolean simulateCrash() {
        return random.nextDouble() < config.crashProbability;
    }

    private boolean simulateLostAck() {
        return random.nextDouble() < config.lostAckProbability;
    }
}
