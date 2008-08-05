public class RunComparison {
    public static void main(String[] args) {
        System.out.println("Running ServiceLoader version:");
        using_serviceloader.Client.main(args);
        System.out.println();
        System.out.println("Running SezPoz version:");
        using_sezpoz.Client.main(args);
    }
}
