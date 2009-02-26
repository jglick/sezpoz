/**
 * Support for declaring, creating, and inspecting indices of annotated Java elements.
 * <p>
 * For example, to permit registration of simple menu items, while
 * making it possible to prepare a menu without loading any of them
 * until they are actually selected:
 * <pre>
 * {@literal @}Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
 * {@literal @}Retention(RetentionPolicy.SOURCE)
 * {@literal @}Indexable(type=ActionListener.class)
 * public {@literal @}interface MenuItem {
 *     String menuName();
 *     String itemName();
 *     String iconPath() default "";
 * }
 * </pre>
 * A concrete registration might look like:
 * <pre>
 * {@literal @}MenuItem(menuName="File", itemName="Print", iconPath=".../print.png")
 * public class PrintAction extends AbstractAction {
 *     public void actionPerformed(ActionEvent e) {...}
 * }
 * </pre>
 * Alternatively:
 * <pre>
 * public class Actions {
 *     {@literal @}MenuItem(menuName="File", itemName="Print")
 *     public static Action print() {...}
 * }
 * </pre>
 * or even:
 * <pre>
 * public class Actions {
 *     {@literal @}MenuItem(menuName="File", itemName="Print")
 *     public static final Action PRINT = ...;
 * }
 * </pre>
 * To create the index on JDK 6, just compile your sources normally with javac.
 * If using JDK 5, simply run apt instead of/in addition to javac.
 * (The processor is in the same JAR as this API and should be autodetected.)
 * <p>
 * Usage is then simple:
 * <pre>
 * for (final {@literal IndexItem<MenuItem,ActionListener>} item :
 *         Index.load(MenuItem.class, ActionListener.class)) {
 *     JMenu menu = new JMenu(item.annotation().menuName());
 *     JMenuItem menuitem = new JMenuItem(item.annotation().itemName());
 *     String icon = item.annotation().iconPath();
 *     if (!icon.equals("")) {
 *          menuitem.setIcon(new ImageIcon(icon));
 *     }
 *     menuitem.addActionListener(new ActionListener() {
 *         public void actionPerformed(ActionEvent e) {
 *             try {
 *                 item.instance().actionPerformed(e);
 *             } catch (InstantiationException x) {
 *                 x.printStackTrace();
 *             }
 *         }
 *     });
 * }
 * </pre>
 */
package net.java.sezpoz;
