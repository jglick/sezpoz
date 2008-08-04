package demo;
import demo.api.MenuItem;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.WindowConstants;
import net.java.sezpoz.Index;
import net.java.sezpoz.IndexItem;
public class Main {
    public static void main(String[] args) {
        JFrame f = new JFrame("Demo");
        f.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        JMenuBar bar = new JMenuBar();
        f.setJMenuBar(bar);
        Map<String,JMenu> menus = new HashMap<String,JMenu>();
        for (final IndexItem<MenuItem,ActionListener> item : Index.load(MenuItem.class, ActionListener.class)) {
            String menuName = item.annotation().menu();
            JMenu menu = menus.get(menuName);
            if (menu == null) {
                menu = new JMenu(menuName);
                menus.put(menuName, menu);
                bar.add(menu);
            }
            JMenuItem menuItem = new JMenuItem(item.annotation().name());
            menu.add(menuItem);
            menuItem.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    try {
                        item.instance().actionPerformed(e);
                    } catch (InstantiationException x) {
                        x.printStackTrace();
                    }
                }
            });
        }
        f.add(new JLabel("Nothing here, try menus"));
        f.pack();
        f.setVisible(true);
    }
}
