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

package net.java.sezpoz.impl;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

public class IndexerTest {

    protected File dir, src, clz;
    @Before public void setUp() throws Exception {
        dir = TestUtils.getWorkDir(this);
        TestUtils.clearDir(dir);
        src = new File(dir, "src");
        clz = new File(dir, "clz");
    }

    @Test public void basicUsage() throws Exception {
        File src1 = new File(dir, "src1");
        TestUtils.makeSource(src1, "api.MenuItem",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable(type=javax.swing.Action.class)",
                "public @interface MenuItem {",
                "String menuName();",
                "String itemName();",
                "String iconPath() default \"\";",
                "}");
        File clz1 = new File(dir, "clz1");
        TestUtils.runApt(src1, null, clz1, null, null);
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "impl.PrintAction",
                "@api.MenuItem(menuName=\"File\", itemName=\"Print\", iconPath=\".../print.png\")",
                "@Deprecated", // make sure this is ignored!
                "public class PrintAction extends javax.swing.AbstractAction {",
                "public void actionPerformed(java.awt.event.ActionEvent e) {}",
                "}");
        File clz2 = new File(dir, "clz2");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null);
        assertEquals(Collections.singletonMap("api.MenuItem", Collections.singletonList(
                "impl.PrintAction{iconPath=.../print.png, itemName=Print, menuName=File}"
                )), TestUtils.findMetadata(clz2));
    }

    @Test public void strangeAnnStringVals() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "String x();",
                "}");
        TestUtils.makeSource(src, "y.C",
                "@x.A(x=\"foo\\\\\\\"\\n\")",
                "public class C {}");
        TestUtils.runApt(src, null, clz, null, null);
        assertEquals(Collections.singletonMap("x.A", Collections.singletonList(
                "y.C{x=foo\\\"\n}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void compositeVals() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "Other other();",
                "Other[] others();",
                "int i();",
                "boolean b();",
                "char c();",
                "}");
        TestUtils.makeSource(src, "x.Other",
                "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)",
                "public @interface Other {",
                "int v();",
                "}");
        TestUtils.makeSource(src, "y.C",
                "import x.*;",
                "@A(other=@Other(v=1),others=@Other(v=2),i=3,b=true,c='x')",
                "public class C {}");
        TestUtils.runApt(src, null, clz, null, null);
        assertEquals(Collections.singletonMap("x.A", Collections.singletonList(
                "y.C{b=true, c=x, i=3, other=@x.Other{v=1}, others=[@x.Other{v=2}]}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void defaultValsNotWritten() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "int i() default 0;",
                "}");
        TestUtils.makeSource(src, "x.B",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface B {",
                "A[] as() default {};",
                "}");
        TestUtils.makeSource(src, "y.C1",
                "@x.A(i=33)",
                "public class C1 {}");
        TestUtils.makeSource(src, "y.C2",
                "@x.A()",
                "public class C2 {}");
        TestUtils.makeSource(src, "y.C3",
                "@x.A",
                "public class C3 {}");
        TestUtils.makeSource(src, "y.C4",
                "@x.B",
                "public class C4 {}");
        TestUtils.makeSource(src, "y.C5",
                "import x.*;",
                "@B(as={@A, @A(), @A(i=20)})",
                "public class C5 {}");
        TestUtils.runApt(src, null, clz, null, null);
        Map<String,List<String>> expected = new HashMap<String,List<String>>();
        expected.put("x.A", Arrays.asList(
            "y.C1{i=33}",
            "y.C2{}",
            "y.C3{}"
        ));
        expected.put("x.B", Arrays.asList(
            "y.C4{}",
            "y.C5{as=[@x.A{}, @x.A{}, @x.A{i=20}]}"
        ));
        assertEquals(expected, TestUtils.findMetadata(clz));
    }

    @Test public void serializeExoticConstants() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "Class clazz();",
                "E[] enoom();",
                "}");
        TestUtils.makeSource(src, "x.E",
                "public enum E {ONE, TWO}");
        TestUtils.makeSource(src, "y.C",
                "import x.*;",
                "@A(clazz=String.class,enoom={E.ONE,E.TWO})",
                "public class C {}");
        TestUtils.runApt(src, null, clz, null, null);
        assertEquals(Collections.singletonMap("x.A", Collections.singletonList(
                "y.C{clazz=java.lang.String, enoom=[ONE, TWO]}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void methodsAndFields() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "boolean b();",
                "}");
        TestUtils.makeSource(src, "y.C",
                "import x.*;",
                "public class C {",
                "@A(b=true)",
                "public static final Object F = null;",
                "@A(b=false)",
                "public static Object m() {return null;}",
                "}");
        TestUtils.runApt(src, null, clz, null, null);
        assertEquals(Collections.singletonMap("x.A", Arrays.asList(
                "y.C#F{b=true}",
                "y.C#m(){b=false}"
        )), TestUtils.findMetadata(clz));
    }

    @Test public void nestedClasses() throws Exception {
        TestUtils.makeSource(src, "x.Outer",
                "import java.lang.annotation.*;",
                "public class Outer {",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "@Retention(RetentionPolicy.SOURCE)",
                "public @interface A {",
                "Class<?> type();",
                "}",
                "}");
        TestUtils.makeSource(src, "y.Auter",
                "public class Auter {",
                "public static class T {}",
                "@x.Outer.A(type=T.class)",
                "public static class C {}",
                "}");
        TestUtils.runApt(src, null, clz, null, null);
        assertEquals(Collections.singletonMap("x.Outer$A", Collections.singletonList(
                "y.Auter$C{type=y.Auter$T}"
                )), TestUtils.findMetadata(clz));
    }

    /* Uncompilable anyway: "annotation value must be a class literal"
    @Test public void exoticClassConstants() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "Class[] clazz();",
                "}");
        TestUtils.makeSource(src, "y.C",
                "import x.*;",
                "@A(clazz={Integer.class, Integer.TYPE})",
                "public class C {}");
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
                "y.C{clazz=[java.lang.Integer, int]}"
                )), TestUtils.findMetadata(clz));
    }
     */

    @Test public void incrementalCompilation() throws Exception {
        File src1 = new File(dir, "src1");
        TestUtils.makeSource(src1, "Thing",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface Thing {",
                "String name();",
                "}");
        File clz1 = new File(dir, "clz1");
        TestUtils.runApt(src1, null, clz1, null, null);
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "Impl1",
                "@Thing(name=\"one\")",
                "public class Impl1 {}");
        File clz2 = new File(dir, "clz2");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null);
        assertEquals(Collections.singletonMap("Thing", Collections.singletonList(
                "Impl1{name=one}"
                )), TestUtils.findMetadata(clz2));
        TestUtils.makeSource(src2, "Impl2",
                "@Thing(name=\"two\")",
                "public class Impl2 {}");
        TestUtils.runApt(src2, "Impl2", clz2, new File[] {clz1}, null);
        assertEquals(Collections.singletonMap("Thing", Arrays.asList(
                "Impl1{name=one}",
                "Impl2{name=two}"
                )), TestUtils.findMetadata(clz2));
        TestUtils.makeSource(src2, "Impl2",
                "@Thing(name=\"Two\")",
                "public class Impl2 {}");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null);
        assertEquals(Collections.singletonMap("Thing", Arrays.asList(
                "Impl1{name=one}",
                "Impl2{name=Two}"
                )), TestUtils.findMetadata(clz2));
    }

    // XXX the following should be moved to IndexerTestBase when Indexer5 implements these things:

    @Test public void nonPublic() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {}");
        TestUtils.makeSource(src, "y.C1", "@x.A public class C1 {}");
        TestUtils.makeSource(src, "y.C2", "@x.A class C2 {}");
        TestUtils.runApt(src, "A|C1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "public");
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 {@x.A protected static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M4", "public class M4 {@x.A private static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M5", "class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.runApt(src, "A|M1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "public");
        TestUtils.runAptExpectingErrors(src, "A|M3", clz, null, "public");
        TestUtils.runAptExpectingErrors(src, "A|M4", clz, null, "public");
        TestUtils.runAptExpectingErrors(src, "A|M5", clz, null, "public");
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A static final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 {@x.A private static final Object f = null;}");
        TestUtils.makeSource(src, "y.F4", "class F4 {@x.A public static final Object f = null;}");
        TestUtils.runApt(src, "A|F1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "public");
        TestUtils.runAptExpectingErrors(src, "A|F3", clz, null, "public");
        TestUtils.runAptExpectingErrors(src, "A|F4", clz, null, "public");
    }

    @Test public void inappropriateModifiersOrArgs() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A {}");
        TestUtils.makeSource(src, "y.C1", "@x.A public class C1 {}");
        TestUtils.makeSource(src, "y.C2", "@x.A public abstract class C2 {}");
        TestUtils.makeSource(src, "y.C3", "@x.A public class C3 {private C3() {}}");
        TestUtils.makeSource(src, "y.C4", "@x.A public class C4 {public C4(int x) {}}");
        TestUtils.runApt(src, "A|C1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "abstract");
        TestUtils.runAptExpectingErrors(src, "A|C3", clz, null, "constructor");
        TestUtils.runAptExpectingErrors(src, "A|C4", clz, null, "constructor");
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A public Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 {@x.A public static Object m(int x) {return null;}}");
        TestUtils.runApt(src, "A|M1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "static");
        TestUtils.runAptExpectingErrors(src, "A|M3", clz, null, "parameters");
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A public final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 {@x.A public static Object f = null;}");
        TestUtils.runApt(src, "A|F1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "static");
        TestUtils.runAptExpectingErrors(src, "A|F3", clz, null, "final");
        TestUtils.makeSource(src, "y.N1", "public class N1 {@x.A public static class N {}}");
        TestUtils.makeSource(src, "y.N2", "public class N2 {@x.A public class N {}}");
        TestUtils.runApt(src, "A|N1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|N2", clz, null, "static");
        /* XXX 269 processors will not even see this:
        TestUtils.makeSource(src, "y.N3", "public class N3 {void m() {@x.A class N {}}}");
        TestUtils.runAptExpectingErrors(src, "A|N3", clz, null, "static", useJsr199());
         */
    }

    @Test public void incompatibleType() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable(type=Runnable.class)",
                "public @interface A {}");
        TestUtils.makeSource(src, "y.C1", "@x.A public class C1 implements Runnable {public void run() {}}");
        TestUtils.makeSource(src, "y.C2", "@x.A public class C2 {}");
        TestUtils.runApt(src, "A|C1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "Runnable");
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Runnable m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 implements Runnable {@x.A public static M3 m() {return null;} public void run() {}}");
        TestUtils.runApt(src, "A|M1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "Runnable");
        TestUtils.runApt(src, "A|M3", clz, null, null);
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Runnable f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 implements Runnable {@x.A public static final F3 f = null; public void run() {}}");
        TestUtils.runApt(src, "A|F1", clz, null, null);
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "Runnable");
        TestUtils.runApt(src, "A|F3", clz, null, null);
    }

    @Test public void inappropriateIndexable() throws Exception {
        TestUtils.makeSource(src, "x.A1",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A1 {}");
        TestUtils.runApt(src, "A1", clz, null, null);
        TestUtils.makeSource(src, "x.A2",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@Inherited",
                "@net.java.sezpoz.Indexable",
                "public @interface A2 {}");
        TestUtils.runAptExpectingErrors(src, "A2", clz, null, "@Inherited");
        TestUtils.makeSource(src, "x.A3",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A3 {}");
        TestUtils.runAptExpectingErrors(src, "A3", clz, null, "CONSTRUCTOR");
        TestUtils.makeSource(src, "x.A4",
                "@net.java.sezpoz.Indexable",
                "@Retention(RetentionPolicy.SOURCE)",
                "public @interface A4 {}");
        TestUtils.runAptExpectingErrors(src, "A4", clz, null, "@Target");
        TestUtils.makeSource(src, "x.A5",
                "import java.lang.annotation.*;",
                "@Target({})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A5 {}");
        TestUtils.runAptExpectingErrors(src, "A5", clz, null, "@Target");
        TestUtils.makeSource(src, "x.A6",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A6 {}");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        TestUtils.runApt(src, "A6", clz, null, baos);
        baos.flush();
        assertTrue("output warns about @Retention: " + baos, baos.toString().contains("@Retention"));
    }

}
