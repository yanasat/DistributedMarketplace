package marketplace;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.yaml.snakeyaml.Yaml;

public class MarketplaceConfig {
    public MarketplaceInfo marketplace;
    public OrderSettings orders;
    public List<String> sellers;
    public List<String> products;
    public SimulationSettings simulation;

    public static class MarketplaceInfo {
        public int port;
        public String name;
    }

    public static class OrderSettings {
        public int arrival_rate_ms;
        public int max_orders;
        public int timeout_ms;
    }

    public static class SimulationSettings {
        public boolean enable_logging;
        public String log_level;
        public boolean performance_monitoring;
    }

    public static MarketplaceConfig load(String filePath) {
        try (InputStream in = Files.newInputStream(Paths.get(filePath))) {
            Yaml yaml = new Yaml();
            return yaml.loadAs(in, MarketplaceConfig.class);
        } catch (Exception e) {
            // Fallback to default configuration
            System.out.println("Warning: Could not load config file " + filePath + ", using defaults");
            return createDefault();
        }
    }

    private static MarketplaceConfig createDefault() {
        MarketplaceConfig config = new MarketplaceConfig();
        
        config.marketplace = new MarketplaceInfo();
        config.marketplace.port = 7777;
        config.marketplace.name = "Marketplace-Default";
        
        config.orders = new OrderSettings();
        config.orders.arrival_rate_ms = 2000;
        config.orders.max_orders = 5;
        config.orders.timeout_ms = 3000;
        
        config.sellers = List.of(
            "tcp://127.0.0.1:5555",
            "tcp://127.0.0.1:5556",
            "tcp://127.0.0.1:5557",
            "tcp://127.0.0.1:5558",
            "tcp://127.0.0.1:5559"
        );
        
        config.products = List.of("laptop", "smartphone", "tablet");
        
        config.simulation = new SimulationSettings();
        config.simulation.enable_logging = true;
        config.simulation.log_level = "INFO";
        config.simulation.performance_monitoring = true;
        
        return config;
    }

    @Override
    public String toString() {
        return String.format("MarketplaceConfig{port=%d, name='%s', orders=%d, sellers=%d}", 
                           marketplace.port, marketplace.name, orders.max_orders, sellers.size());
    }
}