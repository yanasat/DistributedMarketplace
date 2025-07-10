// Simple integration test to verify multi-process setup works
// This will be your first integration test

import marketplace.Marketplace;
import java.util.List;

public class IntegrationTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Integration Test: Multi-Process Communication ===");
        
        // List of seller endpoints (should match what you start manually)
        List<String> sellerEndpoints = List.of(
            "tcp://localhost:5555",
            "tcp://localhost:5556",
            "tcp://localhost:5557",
            "tcp://localhost:5558",
            "tcp://localhost:5559"
        );
        
        System.out.println("Testing connection to sellers: " + sellerEndpoints);
        
        // Create marketplace instance
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Test 1: Single order
        System.out.println("\n--- Test 1: Single Order ---");
        marketplace.placeOrder("testProduct1");
        
        // Test 2: Multiple orders quickly
        System.out.println("\n--- Test 2: Multiple Quick Orders ---");
        for (int i = 0; i < 3; i++) {
            marketplace.placeOrder("testProduct" + (i + 2));
            Thread.sleep(500); // Short delay
        }
        
        // Test 3: Same product multiple times (should test inventory)
        System.out.println("\n--- Test 3: Same Product Multiple Times ---");
        for (int i = 0; i < 3; i++) {
            marketplace.placeOrder("sameProduct");
            Thread.sleep(1000);
        }
        
        System.out.println("\n=== Integration Test Complete ===");
        System.out.println("Check the seller windows for their responses.");
    }
}
