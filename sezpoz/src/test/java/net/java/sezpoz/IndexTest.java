/*
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License ("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file.  This particular file is subject to the "Classpath"
 * exception as provided in the GPL Version 2 section of the License file
 * that accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is SezPoz. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Copyright 2006-2011 Oracle
 * Corporation. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package net.java.sezpoz;

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.util.Arrays;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.swing.Action;
import net.java.sezpoz.impl.TestUtils;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Check that loading of an index works.
 */
public class IndexTest {

    private File dir, src, clz;
    private ClassLoader loader;
    @Before public void setUp() throws Exception {
        dir = TestUtils.getWorkDir(this);
        TestUtils.clearDir(dir);
        src = new File(dir, "src");
        clz = new File(dir, "clz");
        clz.mkdirs();
        loader = new URLClassLoader(new URL[] {clz.toURI().toURL()});
    }

    @SuppressWarnings("unchecked")
    @Test public void basicUsage() throws Exception {
        TestUtils.makeSource(src, "api.MenuItem",
                "import java.lang.annotation.*;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable(type=javax.swing.Action.class)",
                "public @interface MenuItem {",
                "String menuName();",
                "String itemName();",
                "String iconPath() default \"\";",
                "}");
        TestUtils.makeSource(src, "impl.PrintAction",
                "@api.MenuItem(menuName=\"File\", itemName=\"Print\", iconPath=\".../print.png\")",
                "public class PrintAction extends javax.swing.AbstractAction {",
                "public void actionPerformed(java.awt.event.ActionEvent e) {}",
                "}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        Class menuItemClazz = loader.loadClass("api.MenuItem");
        Index index = Index.load(menuItemClazz, Action.class, loader);
        Iterator<IndexItem> it = index.iterator();
        assertTrue(it.hasNext());
        IndexItem item = it.next();
        assertFalse(it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException x) {/*OK*/}
        Annotation ann = item.annotation();
        assertEquals(menuItemClazz, ann.annotationType());
        assertEquals("File", menuItemClazz.getMethod("menuName").invoke(ann));
        assertEquals("Print", menuItemClazz.getMethod("itemName").invoke(ann));
        assertEquals(".../print.png", menuItemClazz.getMethod("iconPath").invoke(ann));
        assertEquals("impl.PrintAction", item.className());
        assertEquals(null, item.memberName());
        assertEquals(ElementType.TYPE, item.kind());
        Class implClazz = loader.loadClass("impl.PrintAction");
        assertEquals(implClazz, item.element());
        Annotation live = implClazz.getAnnotation(menuItemClazz);
        assertEquals("live and proxy annotations match", ann, live);
        assertEquals("live and proxy annotations match in opposite direction", live, ann);
        assertEquals("proxy annotation equal to itself", ann, ann);
        assertEquals("live and proxy annotations have same hash code", ann.hashCode(), live.hashCode());
        assertNotSame("but live and proxy annotations are not identical", ann, live);
        Object o = item.instance();
        assertEquals(implClazz, o.getClass());
        assertSame("caches instance", o, item.instance());
        Index index2 = Index.load(menuItemClazz, Action.class, loader);
        it = index2.iterator();
        assertTrue(it.hasNext());
        IndexItem item2 = it.next();
        assertFalse(it.hasNext());
        assertEquals("IndexItem.equals works naturally", item, item2);
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Indexable
    public @interface Marker {
        String stuff();
    }
    @Marker(stuff="hello")
    public static class Marked1 {}
    @Marker(stuff="goodbye")
    public static class Marked2 {}
    @Test public void staticallyKnownAnnotation() throws Exception {
        TestUtils.makeSource(src, "impl.C",
                "import " + Marker.class.getName().replace('$', '.') + ";",
                "@Marker(stuff=\"hello\")",
                "public class C {}");
        TestUtils.runApt(src, null, clz, new File[] {
            new File(URI.create(Marker.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm()))
        }, null, false);
        int cnt = 0;
        for (IndexItem<Marker,Object> item : Index.load(Marker.class, Object.class, loader)) {
            cnt++;
            assertEquals("impl.C", ((Class) item.element()).getName());
            assertEquals("impl.C", item.instance().getClass().getName());
            Marker proxy = item.annotation();
            assertEquals(Marker.class, proxy.annotationType());
            assertEquals("hello", proxy.stuff());
            Marker real1 = Marked1.class.getAnnotation(Marker.class), real2 = Marked2.class.getAnnotation(Marker.class);
            assertEquals(real1, proxy);
            assertEquals(proxy, real1);
            assertEquals(real1.hashCode(), proxy.hashCode());
            assertFalse(real2.equals(proxy));
            assertFalse(proxy.equals(real2));
        }
        assertEquals(1, cnt);
    }

    @SuppressWarnings("unchecked")
    @Test public void methodsAndFields() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {}");
        TestUtils.makeSource(src, "y.C",
                "public class C {",
                "@x.A",
                "public static final Object F = 1;",
                "@x.A",
                "public static Object m() {return 2;}",
                "}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        int cnt = 0;
        for (IndexItem item : Index.load(a, Object.class, loader)) {
            cnt++;
            assertEquals("y.C", item.className());
            if (item.kind() == ElementType.FIELD) {
                assertEquals("F", item.memberName());
                assertEquals(1, item.instance());
            } else {
                assertEquals(ElementType.METHOD, item.kind());
                assertEquals("m", item.memberName());
                assertEquals(2, item.instance());
            }
        }
        assertEquals(2, cnt);
    }

    @Test public void multipleCodeSources() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {}");
        TestUtils.makeSource(src, "y.C1",
                "@x.A",
                "public class C1 {}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "z.C2",
                "@x.A",
                "public class C2 {}");
        File clz2 = new File(dir, "clz2");
        clz2.mkdirs();
        TestUtils.runApt(src2, null, clz2, new File[] {clz}, null, false);
        loader = new URLClassLoader(new URL[] {clz.toURI().toURL(), clz2.toURI().toURL()});
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        Iterator it = Index.load(a, Object.class, loader).iterator();
        assertTrue(it.hasNext());
        IndexItem item = (IndexItem) it.next();
        assertEquals("y.C1", item.instance().getClass().getName());
        assertTrue(it.hasNext());
        item = (IndexItem) it.next();
        assertEquals("z.C2", item.instance().getClass().getName());
        assertFalse(it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException x) {/*OK*/}
    }

    @Test public void codeSourceOverlapping() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "int x();",
                "}");
        TestUtils.makeSource(src, "y.C1",
                "@x.A(x=1)",
                "public class C1 {}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "y.C1",
                "@x.A(x=2)",
                "public class C1 {}");
        File clz2 = new File(dir, "clz2");
        clz2.mkdirs();
        TestUtils.runApt(src2, null, clz2, new File[] {clz}, null, false);
        loader = new URLClassLoader(new URL[] {clz.toURI().toURL(), clz2.toURI().toURL()});
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        Iterator it = Index.load(a, Object.class, loader).iterator();
        assertTrue(it.hasNext());
        IndexItem item = (IndexItem) it.next();
        assertEquals("y.C1", item.instance().getClass().getName());
        assertEquals(1, a.getMethod("x").invoke(item.annotation()));
        assertFalse("y.C1 only loaded once", it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException x) {/*OK*/}
    }

    @Test public void heavyOverlap() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "int x();",
                "}");
        TestUtils.makeSource(src, "y.C1",
                "@x.A(x=1)",
                "public class C1 {}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        URL[] urls = new URL[9]; // too slow to make this really big
        for (int i = 0; i < urls.length; i++) {
            urls[i] = new URL(new URL("http://" + i + ".nowhere.net/"), "", new URLStreamHandler() {
                protected URLConnection openConnection(URL u) throws IOException {
                    URL u2 = new URL(clz.toURI().toURL(), u.getPath().substring(1));
                    return u2.openConnection();
                }
            });
        }
        loader = new URLClassLoader(urls);
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        Iterator it = Index.load(a, Object.class, loader).iterator();
        assertTrue(it.hasNext());
        IndexItem item = (IndexItem) it.next();
        assertEquals("y.C1", item.instance().getClass().getName());
        assertEquals(1, a.getMethod("x").invoke(item.annotation()));
        assertFalse("y.C1 only loaded once", it.hasNext());
        try {
            it.next();
            fail();
        } catch (NoSuchElementException x) {/*OK*/}
    }

    @Test public void defaultValues() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "int x() default 5;",
                "}");
        TestUtils.makeSource(src, "y.C",
                "import x.A;",
                "public class C {",
                "@A",
                "public static void m1() {}",
                "@A(x=5)",
                "public static void m2() {}",
                "@A(x=17)",
                "public static void m3() {}",
                "}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        int cnt = 0;
        Annotation[] proxyAnns = new Annotation[3];
        Annotation[] liveAnns = new Annotation[3];
        for (IndexItem item : Index.load(a, Object.class, loader)) {
            cnt++;
            assertEquals("y.C", item.className());
            Annotation ann = item.annotation();
            Method x = a.getMethod("x");
            int index;
            if (item.memberName().equals("m1")) {
                index = 0;
                assertEquals(5, x.invoke(ann));
            } else if (item.memberName().equals("m2")) {
                index = 1;
                assertEquals(5, x.invoke(ann));
            } else {
                assertEquals("m3", item.memberName());
                index = 2;
                assertEquals(17, x.invoke(ann));
            }
            proxyAnns[index] = ann;
            liveAnns[index] = item.element().getAnnotation(a);
            assertNotNull(liveAnns[index]);
        }
        assertEquals(3, cnt);
        Set<Annotation> all = new HashSet<Annotation>();
        all.addAll(Arrays.asList(proxyAnns));
        all.addAll(Arrays.asList(liveAnns));
        assertEquals("all annotations are either with x=5 or x=17", 2, all.size());
        Map<Annotation,Void> all2 = new IdentityHashMap<Annotation,Void>();
        for (Annotation ann : proxyAnns) {
            all2.put(ann, null);
        }
        for (Annotation ann : liveAnns) {
            all2.put(ann, null);
        }
        assertEquals(6, all2.size());
    }

    @SuppressWarnings("unchecked")
    @Test public void complexStructure() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "B b();",
                "B[] bs();",
                "int[] xs();",
                "E e();",
                "Class c();",
                "class Nested {}",
                "}");
        TestUtils.makeSource(src, "x.B",
                "import java.lang.annotation.*;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "public @interface B {",
                "int x();",
                "int[] ys() default {1, 2};",
                "E[] es() default {};",
                "}");
        TestUtils.makeSource(src, "x.E",
                "public enum E {",
                "CHOCOLATE, VANILLA, STRAWBERRY",
                "}");
        TestUtils.makeSource(src, "y.C",
                "import x.*;",
                "@A(",
                "b=@B(x=1),",
                "bs={@B(x=2), @B(x=3, ys=17, es=E.VANILLA)},",
                "xs={6, 7},",
                "e=E.CHOCOLATE,",
                "c=A.Nested.class",
                ")",
                "public class C {}");
        TestUtils.runApt(src, null, clz, new File[0], null, false);
        Class<? extends Annotation> a = loader.loadClass("x.A").asSubclass(Annotation.class);
        IndexItem item = Index.load(a, Object.class, loader).iterator().next();
        Class<?> b = loader.loadClass("x.B");
        Class e = loader.loadClass("x.E");
        Annotation ann = item.annotation();
        // Test all structural elements:
        Object bb = a.getMethod("b").invoke(ann);
        assertTrue(b.isAssignableFrom(bb.getClass()));
        assertEquals(1, b.getMethod("x").invoke(bb));
        int[] ys = (int[]) b.getMethod("ys").invoke(bb);
        assertEquals(2, ys.length);
        assertEquals(1, ys[0]);
        assertEquals(2, ys[1]);
        Object[] es = (Object[]) b.getMethod("es").invoke(bb);
        assertEquals(e, es.getClass().getComponentType());
        assertEquals(0, es.length);
        Object[] bs = (Object[]) a.getMethod("bs").invoke(ann);
        assertEquals(b, bs.getClass().getComponentType());
        assertEquals(2, bs.length);
        bb = bs[0];
        assertEquals(2, b.getMethod("x").invoke(bb));
        bb = bs[1];
        assertEquals(3, b.getMethod("x").invoke(bb));
        ys = (int[]) b.getMethod("ys").invoke(bb);
        assertEquals(1, ys.length);
        assertEquals(17, ys[0]);
        es = (Object[]) b.getMethod("es").invoke(bb);
        assertEquals(e, es.getClass().getComponentType());
        assertEquals(1, es.length);
        assertEquals(e.getField("VANILLA").get(null), es[0]);
        int[] xs = (int[]) a.getMethod("xs").invoke(ann);
        assertEquals(2, xs.length);
        assertEquals(6, xs[0]);
        assertEquals(7, xs[1]);
        assertEquals(e.getField("CHOCOLATE").get(null), a.getMethod("e").invoke(ann));
        assertEquals(loader.loadClass("x.A$Nested"), a.getMethod("c").invoke(ann));
        // Check also object comparisons:
        Annotation live = item.element().getAnnotation(a);
        assertEquals(live, ann);
        assertEquals(ann, live);
        assertEquals(live.hashCode(), ann.hashCode());
    }

    // XXX need to test:
    // - verification that interface is indexable
    // - verification that instance type is valid (try with Void also)
    // - annotation values incl. newlines, quotes, etc.
    // - element() and instance() are truly lazy
    // - access permissions
    // - Class constants can be loaded from different class loader than annotation

}
