package demo.plugin1;
import demo.api.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
@MenuItem(name="Exit", menu="File")
public class ExitAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        System.exit(0);
    }
}
