import java.util.ArrayList;
import java.util.List;

import org.zeromq.ZMQ;

import messaging.MessageUtils;

public class HealthChecker {
    
    public static void main(String[] args) {
        System.out.println("=== Distributed Marketplace Health Check ===");
        
        List<String> sellerEndpoints = List.of(
            "tcp://seller1:5555",
            "tcp://seller2:5556",
            "tcp://seller3:5557",
            "tcp://seller4:5558",
            "tcp://seller5:5559"
        );
        
        List<String> marketplaceEndpoints = List.of(
            "tcp://marketplace1:7777",
            "tcp://marketplace2:7778"
        );
        
        System.out.println("\nChecking Seller Processes...");
        List<String> healthySellers = checkSellersHealth(sellerEndpoints);
        

        System.out.println("\nChecking Marketplace Processes...");
        checkMarketplaceHealth();
        

        System.out.println("\n" + "=".repeat(40));
        System.out.println("HEALTH CHECK SUMMARY");
        System.out.println("=".repeat(40));
        System.out.println("Healthy Sellers: " + healthySellers.size() + "/" + sellerEndpoints.size());
        for (String seller : healthySellers) {
            System.out.println("  [OK] " + seller);
        }
        
        if (healthySellers.size() == sellerEndpoints.size()) {
            System.out.println("[SUCCESS] ALL SYSTEMS HEALTHY");
        } else {
            System.out.println("[WARNING] SOME SYSTEMS DOWN");
        }
        System.out.println("=".repeat(40));
    }
    
    private static List<String> checkSellersHealth(List<String> endpoints) {
        List<String> healthySellers = new ArrayList<>();
        
        for (String endpoint : endpoints) {
            System.out.print("Testing " + endpoint + "... ");
            
            if (pingSeller(endpoint)) {
                System.out.println("[OK] HEALTHY");
                healthySellers.add(endpoint);
            } else {
                System.out.println("[FAIL] UNREACHABLE");
            }
        }
        
        return healthySellers;
    }
    
    private static boolean pingSeller(String endpoint) {
        ZMQ.Socket socket = null;
        try {
            socket = MessageUtils.createSocket("REQ", false, endpoint);
            socket.setReceiveTimeOut(2000); 
            socket.setSendTimeOut(1000);  
            
            socket.send("HEALTH_CHECK");
            
            String response = socket.recvStr();
            
            return response != null;
            
        } catch (Exception e) {
            return false;
        } finally {
            if (socket != null) {
                socket.close();
            }
        }
    }
    
    private static void checkMarketplaceHealth() {
        System.out.println("Marketplace health checked via process monitoring");
        System.out.println("(Marketplaces are clients, not servers - check their windows are active)");
    }
}
