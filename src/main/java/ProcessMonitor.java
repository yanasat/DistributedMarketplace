// Process monitoring utility for tracking performance across distributed processes
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class ProcessMonitor {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final AtomicInteger totalOrders = new AtomicInteger(0);
    private static final AtomicInteger successfulOrders = new AtomicInteger(0);
    private static final AtomicInteger failedOrders = new AtomicInteger(0);
    private static final AtomicLong totalResponseTime = new AtomicLong(0);
    private static final AtomicInteger responseCount = new AtomicInteger(0);
    
    public static void logOrderStart(String processName, String orderId, String product) {
        totalOrders.incrementAndGet();
        System.out.println(String.format("[%s] [%s] ORDER_START: %s for product %s", 
            getCurrentTimestamp(), processName, orderId, product));
    }
    
    public static void logOrderSuccess(String processName, String orderId, long responseTimeMs) {
        successfulOrders.incrementAndGet();
        addResponseTime(responseTimeMs);
        System.out.println(String.format("\033[32m[%s] [%s] ORDER_SUCCESS: %s (took %dms)\033[0m", 
            getCurrentTimestamp(), processName, orderId, responseTimeMs));
        printStats();
    }
    
    public static void logOrderFailure(String processName, String orderId, long responseTimeMs) {
        failedOrders.incrementAndGet();
        addResponseTime(responseTimeMs);
        System.out.println(String.format("\033[31m[%s] [%s] ORDER_FAILURE: %s (took %dms)\033[0m", 
            getCurrentTimestamp(), processName, orderId, responseTimeMs));
        printStats();
    }
    
    public static void logSellerResponse(String processName, String sellerEndpoint, String response, long responseTimeMs) {
        addResponseTime(responseTimeMs);
        System.out.println(String.format("[%s] [%s] SELLER_RESPONSE: %s -> %s (%dms)", 
            getCurrentTimestamp(), processName, sellerEndpoint, response, responseTimeMs));
    }
    
    public static void logProcessStart(String processName, String endpoint) {
        System.out.println(String.format("[%s] [%s] PROCESS_START: Listening on %s", 
            getCurrentTimestamp(), processName, endpoint));
    }
    
    public static void logRollback(String processName, String orderId, int sellersToRollback) {
        System.out.println(String.format("[%s] [%s] ROLLBACK: %s affecting %d sellers", 
            getCurrentTimestamp(), processName, orderId, sellersToRollback));
    }
    
    private static void addResponseTime(long responseTimeMs) {
        totalResponseTime.addAndGet(responseTimeMs);
        responseCount.incrementAndGet();
    }
    
    private static void printStats() {
        int total = totalOrders.get();
        int successful = successfulOrders.get();
        int failed = failedOrders.get();
        int responses = responseCount.get();
        long avgResponseTime = responses > 0 ? totalResponseTime.get() / responses : 0;
        
        double successRate = total > 0 ? (successful * 100.0 / total) : 0;
        
        System.out.println(String.format("\033[33m[%s] [STATS] Orders: %d total, %d successful (%.1f%%), %d failed | Avg response: %dms\033[0m", 
            getCurrentTimestamp(), total, successful, successRate, failed, avgResponseTime));
    }
    
    private static String getCurrentTimestamp() {
        return LocalDateTime.now().format(TIMESTAMP);
    }
    
    public static void printFinalStats() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("FINAL PERFORMANCE STATISTICS");
        System.out.println("=".repeat(50));
        printStats();
        System.out.println("=".repeat(50));
    }
}
