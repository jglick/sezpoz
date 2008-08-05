package demo.plugin2;
import demo.api.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
public class LottaActions {
    @MenuItem(name="Say Hello", menu="Tools")
    public static final ActionListener HELLO = make("Hello!");
    @MenuItem(name="Say Goodbye", menu="Tools")
    public static final ActionListener GOODBYE = make("Good bye!");
    private static ActionListener make(final String message) {
        return new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, message);
            }
        };
    }
}
