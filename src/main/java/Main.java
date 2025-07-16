// Main entry point for the distributed marketplace application
// Starts seller threads and places orders via the marketplace
import marketplace.Marketplace;
import seller.SellerStub;

import java.util.List;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        // List of seller endpoints (addresses)
        List<String> endpoints = List.of("tcp://localhost:5555", "tcp://localhost:5556");

        // Start a thread for each seller endpoint, simulating sellers coming online
        for (String ep : endpoints) {
            new Thread(() -> SellerStub.start(ep)).start();
        }

        // Wait for sellers to initialize
        Thread.sleep(1000);

        // Create a marketplace instance with the seller endpoints
        Marketplace marketplace = new Marketplace(endpoints);

        // Place 5 orders for the product "toni", waiting 2 seconds between each
        for (int i = 0; i < 5; i++) {
            marketplace.placeOrder("toni");
            Thread.sleep(2000);
        }
    }
}
