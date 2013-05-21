SezPoz is a lightweight and easy-to-learn library that lets you perform modular service lookups. It provides some of the same capabilities as (for example) `java.util.ServiceLoader`, Eclipse extension points, and NetBeans `Lookup` and XML layers. However, SezPoz has some special advantages:

1.  The service registrations are made just using type-checked Java annotations. There are no configuration files to edit, and your Java IDE can show you registrations since they are simply usages of an annotation. On JDK 6 (or later), no special build or packaging steps are required (just javac). Looking up services just requires that you have a `ClassLoader` which can "see" all of the "modules" (as with `ServiceLoader`).

2.  You can register individual objects (values of static fields or methods) instead of whole classes.

3.  You can associate static metadata with each implementation, using regular annotation values. The caller can choose to inspect the metadata without loading the actual implementation object (as with Eclipse extension points).

(Why the name? SezPoz "says" the "position" of your services. It is also a "[seznam][1] [poznámek][2]".)

Sources are in the form of Maven projects. To build:

    mvn install

To try the demo application:

    mvn -f demo/app/pom.xml exec:exec

Binaries, sources, and Javadoc can all be downloaded from the Maven Central repository: [Maven repository][3].

For usage from Maven applications, use the artifact `net.java.sezpoz:sezpoz`, for example:

    <dependencies>
      <dependency>
        <groupId>net.java.sezpoz</groupId>
        <artifactId>sezpoz</artifactId>
        <version>…latest available…</version>
      </dependency>
    </dependencies>

See Javadoc for details on particular classes, or just look at demo sources in the `demo` subdirectory in Subversion.

Support for declaring, creating, and inspecting indices of annotated Java elements.

For example, to permit registration of simple menu items, while making it possible to prepare a menu without loading any of them until they are actually selected:

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.SOURCE)
    @Indexable(type=ActionListener.class)
    public @interface MenuItem {
        String menuName();
        String itemName();
        String iconPath() default "";
    }

A concrete registration might look like:

    @MenuItem(menuName="File", itemName="Print", iconPath=".../print.png")
    public class PrintAction extends AbstractAction {
        public void actionPerformed(ActionEvent e) {...}
    }

Alternatively:

    public class Actions {
        @MenuItem(menuName="File", itemName="Print")
        public static Action print() {...}
    }

or even:

    public class Actions {
        @MenuItem(menuName="File", itemName="Print")
        public static final Action PRINT = ...;
    }

To create the index on JDK 6+, just compile your sources normally with javac. (The processor is in the same JAR as this API and should be autodetected.)

Usage is then simple:

    for (final IndexItem<MenuItem,ActionListener> item :
            Index.load(MenuItem.class, ActionListener.class)) {
        JMenu menu = new JMenu(item.annotation().menuName());
        JMenuItem menuitem = new JMenuItem(item.annotation().itemName());
        String icon = item.annotation().iconPath();
        if (!icon.equals("")) {
             menuitem.setIcon(new ImageIcon(icon));
        }
        menuitem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    item.instance().actionPerformed(e);
                } catch (InstantiationException x) {
                    x.printStackTrace();
                }
            }
        });
    }


Known limitations:

1. Incremental compilation is not perfect. If you compile just some sources which are marked with an indexable annotation, these entries will be appended to any existing registrations from previous runs of the compiler. (You should run a clean build if you *delete* annotations from sources.)

2.  The Java language spec currently prohibits recursive annotation definitions, and JDK 6 and 7's javac enforce this. See [bug #6264216][4].

Eclipse-specific notes: make sure annotation processing is enabled at least for any projects registering objects using annotations. Make sure the SezPoz library is in the factory path for annotation processors. You also need to check the box **Run this container's processor in batch mode** from the **Advanced** button in **Java Compiler » Annotation Processing » Factory Path**. There does not appear to be any way for Eclipse to discover processors in the regular classpath as JSR 269 suggests, and there does not appear to be any way to make these settings apply automatically to all projects. Eclipse users are recommended to use javac (e.g. via Maven) to build. [Eclipse Help Page][5] [Eclipse bug #280542][6]

SezPoz is used inside Hudson/Jenkins for the `@Extension` annotation, but this usage does not take advantage of its lazy-loading capability at all.

[GlassFish/DependencyMechanism][7] looks a bit similar. In active use?

[JPF][8] is similar to Eclipse, but can be used in isolation.

[OSGi Declarative Services][9] can use Java annotations to register services, but static metadata looks weak compared to SezPoz.

To be investigated: interoperability with Peaberry and/or Sisu.

 [1]: http://slovnik.seznam.cz/search.py?wd=seznam&amp;lg=cz_en
 [2]: http://slovnik.seznam.cz/search.py?wd=pozn%C3%A1mka&amp;lg=cz_en
 [3]: http://repo1.maven.org/maven2/net/java/sezpoz/sezpoz/
 [4]: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6264216
 [5]: http://help.eclipse.org/ganymede/index.jsp?topic=/org.eclipse.jdt.doc.isv/guide/jdt_apt_getting_started.htm
 [6]: https://bugs.eclipse.org/bugs/show_bug.cgi?id=280542
 [7]: https://wikis.oracle.com/display/GlassFish/DependencyMechanism
 [8]: http://jpf.sourceforge.net/
 [9]: http://wiki.osgi.org/wiki/Declarative_Services
