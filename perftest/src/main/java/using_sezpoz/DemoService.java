package using_sezpoz;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;
@Indexable(type=Runnable.class)
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface DemoService {
    int type();
}
