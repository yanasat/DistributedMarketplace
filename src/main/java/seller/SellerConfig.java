package seller;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class SellerConfig {
    public int port;
    public Map<String, Integer> products;
    public double crashProbability;
    public double lostAckProbability;
    public int avgLatencyMs;
    public double successProbability; // New field for success rate

    public static SellerConfig load(String filePath) {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(in, SellerConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
        }
    }

    @Override
    public String toString() {
        return String.format("SellerConfig{port=%d, crash=%.2f, lostAck=%.2f, latency=%dms, success=%.2f}", 
                           port, crashProbability, lostAckProbability, avgLatencyMs, successProbability);
    }
}