package using_serviceloader;
import java.util.Iterator;
import java.util.ServiceLoader;
public class Client {
    public static void main(String[] args) {
        long start = System.currentTimeMillis();
        ServiceLoader<DemoService> s = ServiceLoader.load(DemoService.class);
        Iterator<DemoService> it = s.iterator();
        while (it.hasNext()) {
            DemoService ds = it.next();
            if (ds.type() == 9917) {
                ds.run();
                break;
            }
        }
        long end = System.currentTimeMillis();
        System.out.printf("Took %dmsec\n", end - start);
    }
}
