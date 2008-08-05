package demo.plugin2;
import demo.api.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JOptionPane;
@MenuItem(name="About", menu="Help")
public class AboutAction implements ActionListener {
    public void actionPerformed(ActionEvent e) {
        JOptionPane.showMessageDialog(null, "This is a demo app!");
    }
}
