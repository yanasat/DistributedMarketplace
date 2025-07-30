import java.util.List;

import marketplace.Marketplace;

public class IntegrationTest {
    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Integration Test: Multi-Process Communication ===");
        
        List<String> sellerEndpoints = List.of(
            "tcp://localhost:5555",
            "tcp://localhost:5556",
            "tcp://localhost:5557",
            "tcp://localhost:5558",
            "tcp://localhost:5559"
        );
        
        System.out.println("Testing connection to sellers: " + sellerEndpoints);
        
        ProcessMonitor.logProcessStart("IntegrationTest", "test-runner");
        
        Marketplace marketplace = new Marketplace(sellerEndpoints);
        
        System.out.println("\n--- Test 1: Single Order ---");
        long startTime = System.currentTimeMillis();
        ProcessMonitor.logOrderStart("IntegrationTest", "TEST-001", "testProduct1");
        
        marketplace.placeOrder("testProduct1", 1);
        
        long processingTime = System.currentTimeMillis() - startTime;
        ProcessMonitor.logOrderSuccess("IntegrationTest", "TEST-001", processingTime);
        
        System.out.println("\n--- Test 2: Multiple Quick Orders ---");
        for (int i = 0; i < 3; i++) {
            String testId = "TEST-00" + (i + 2);
            String product = "testProduct" + (i + 2);
            
            startTime = System.currentTimeMillis();
            ProcessMonitor.logOrderStart("IntegrationTest", testId, product);
            
            marketplace.placeOrder(product, 1);
            
            processingTime = System.currentTimeMillis() - startTime;
            ProcessMonitor.logOrderSuccess("IntegrationTest", testId, processingTime);
            
            Thread.sleep(500);
        }
        
        System.out.println("\n--- Test 3: Same Product Multiple Times ---");
        for (int i = 0; i < 3; i++) {
            String testId = "TEST-00" + (i + 5);
            
            startTime = System.currentTimeMillis();
            ProcessMonitor.logOrderStart("IntegrationTest", testId, "sameProduct");
            
            marketplace.placeOrder("sameProduct", 1);
            
            processingTime = System.currentTimeMillis() - startTime;
            ProcessMonitor.logOrderSuccess("IntegrationTest", testId, processingTime);
            
            Thread.sleep(1000);
        }
        
        System.out.println("\n=== Integration Test Complete ===");
        System.out.println("Check the seller windows for their responses.");
        
        ProcessMonitor.printFinalStats();
    }
}
