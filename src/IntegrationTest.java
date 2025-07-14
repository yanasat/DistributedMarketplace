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
        
        // Initialize monitoring
        ProcessMonitor.logProcessStart("IntegrationTest", "test-runner");
        
        // Create marketplace instance
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        // Test 1: Single order
        System.out.println("\n--- Test 1: Single Order ---");
        long startTime = System.currentTimeMillis();
        ProcessMonitor.logOrderStart("IntegrationTest", "TEST-001", "testProduct1");
        
        marketplace.placeOrder("testProduct1");
        
        long processingTime = System.currentTimeMillis() - startTime;
        ProcessMonitor.logOrderSuccess("IntegrationTest", "TEST-001", processingTime);
        
        // Test 2: Multiple orders quickly
        System.out.println("\n--- Test 2: Multiple Quick Orders ---");
        for (int i = 0; i < 3; i++) {
            String testId = "TEST-00" + (i + 2);
            String product = "testProduct" + (i + 2);
            
            startTime = System.currentTimeMillis();
            ProcessMonitor.logOrderStart("IntegrationTest", testId, product);
            
            marketplace.placeOrder(product);
            
            processingTime = System.currentTimeMillis() - startTime;
            ProcessMonitor.logOrderSuccess("IntegrationTest", testId, processingTime);
            
            Thread.sleep(500); // Short delay
        }
        
        // Test 3: Same product multiple times (should test inventory)
        System.out.println("\n--- Test 3: Same Product Multiple Times ---");
        for (int i = 0; i < 3; i++) {
            String testId = "TEST-00" + (i + 5);
            
            startTime = System.currentTimeMillis();
            ProcessMonitor.logOrderStart("IntegrationTest", testId, "sameProduct");
            
            marketplace.placeOrder("sameProduct");
            
            processingTime = System.currentTimeMillis() - startTime;
            ProcessMonitor.logOrderSuccess("IntegrationTest", testId, processingTime);
            
            Thread.sleep(1000);
        }
        
        System.out.println("\n=== Integration Test Complete ===");
        System.out.println("Check the seller windows for their responses.");
        
        // Print final statistics
        ProcessMonitor.printFinalStats();
    }
}
