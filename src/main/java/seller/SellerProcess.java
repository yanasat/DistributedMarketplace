package seller;

public class SellerProcess {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar <jar-file> <config.yaml>");
            System.exit(1);
        }

        String configFile = args[0];
        SellerConfig config = SellerConfig.load(configFile);
        Seller seller = new Seller(config);
        seller.start();
    }
}
