package using_sezpoz;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
public class Client {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        for (IndexItem<DemoService,Runnable> item : Index.load(DemoService.class, Runnable.class)) {
            if (item.annotation().type() == 9917) {
                try {
                    item.instance().run();
                } catch (InstantiationException x) {
                    x.printStackTrace();
                }
                break;
            }
        }
        long end = System.currentTimeMillis();
        System.out.printf("Took %dmsec\n", end - start);
    }
}
