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

    public static SellerConfig load(String filePath) {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(in, SellerConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load config: " + e.getMessage(), e);
        }
    }
}
