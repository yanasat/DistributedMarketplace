package seller;

import java.io.InputStream;
import java.util.Map;

import org.yaml.snakeyaml.Yaml;

public class SellerConfig {
    public int port;
    public Map<String, Integer> products;
    public double crashProbability;
    public double lostAckProbability;
    public int avgLatencyMs;

    public static SellerConfig load(String filename) {
        try (InputStream in = SellerConfig.class.getClassLoader().getResourceAsStream(filename)) {
            if (in == null) throw new RuntimeException("Config file not found: " + filename);

            Yaml yaml = new Yaml();
            return yaml.loadAs(in, SellerConfig.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to load YAML config: " + e.getMessage(), e);
        }
    }
}
