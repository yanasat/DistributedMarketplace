package seller;

public class SellerProcess {
    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("Usage: java -jar SellerProcess.jar <config.yaml>");
            System.exit(1);
        }
        SellerConfig config = SellerConfig.load(args[0]);
        Seller seller = new Seller(config);
        seller.start();
    }
}
