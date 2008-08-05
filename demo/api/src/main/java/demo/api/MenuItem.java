package demo.api;
import java.awt.event.ActionListener;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import net.java.sezpoz.Indexable;
@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
@Indexable(type=ActionListener.class)
public @interface MenuItem {
    String menu();
    String name();
}
