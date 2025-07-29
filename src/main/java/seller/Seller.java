package seller;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

public class Seller {
    private SellerConfig config;
    private final ProductInventory inventory;
    private final Random random = new Random();
    private static final Logger LOGGER = Logger.getLogger(Seller.class.getName());

    public Seller(SellerConfig config) {
        this.config = config;
        this.inventory = new ProductInventory(config.products);
    }

    public void start() {
        try (ZContext context = new ZContext();
             ZMQ.Socket socket = context.createSocket(SocketType.REP)) {
            socket.bind(String.format("tcp://*:%d", config.port));
            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.info(String.format("Seller listening on port %d", config.port));
            }

            while (!Thread.currentThread().isInterrupted()) {
                processMessage(socket);
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("Error in Seller process: %s", e.getMessage()));
        }
    }

    private void processMessage(ZMQ.Socket socket) {
        String msg = socket.recvStr();
        if (LOGGER.isLoggable(Level.INFO)) {
            LOGGER.info(String.format("Received: %s", msg));
        }

        simulateLatency();
        if (simulateCrash()) {
            LOGGER.warning("Simulating crash: ignoring message.");
            return;
        }
        if (simulateLostAck()) {
            LOGGER.warning("Simulating lost ack: not replying.");
            return;
        }

        String[] parts = msg.split(":");
        String cmd = parts[0];
        String orderId = parts[1];
        String product = parts[2];
        int qty = Integer.parseInt(parts[3]);

        String reply;
        switch (cmd) {
            case "RESERVE":
                reply = inventory.reserve(product, qty) ? String.format("CONFIRMED:%s", orderId) : String.format("REJECTED:%s", orderId);
                break;
            case "COMMIT":
                inventory.commit(product, qty);
                reply = String.format("COMMITTED:%s", orderId);
                break;
            case "CANCEL":
                inventory.rollback(product, qty);
                reply = String.format("ROLLED_BACK:%s", orderId);
                break;
            default:
                reply = String.format("UNKNOWN:%s", orderId);
        }
        socket.send(reply);
    }

    private void simulateLatency() {
        long latency = Math.max(0, (long) (random.nextGaussian() * config.avgLatencyMs));
        if (latency > 100) {
            if (LOGGER.isLoggable(Level.WARNING)) {
                LOGGER.warning(String.format("High latency detected: %dms", latency));
            }
        }
        try {
            Thread.sleep(latency);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.severe("Latency simulation interrupted: " + e.getMessage());
        }
    }

    private boolean simulateCrash() {
        return random.nextDouble() < config.crashProbability;
    }

    private boolean simulateLostAck() {
        return random.nextDouble() < config.lostAckProbability;
    }

    // Dynamische Anpassbarkeit der Parameter
    public void updateConfig(SellerConfig newConfig) {
        this.config = newConfig;
        LOGGER.info("Seller configuration updated dynamically");
    }
}
