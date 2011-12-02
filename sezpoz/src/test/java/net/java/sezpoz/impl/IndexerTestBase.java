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

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

/**
 * Tries to run indexer and confirms that it serializes annotations correctly.
 */
public abstract class IndexerTestBase {

    protected abstract boolean useJsr199();

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
        TestUtils.runApt(src1, null, clz1, null, null, useJsr199());
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "impl.PrintAction",
                "@api.MenuItem(menuName=\"File\", itemName=\"Print\", iconPath=\".../print.png\")",
                "@Deprecated", // make sure this is ignored!
                "public class PrintAction extends javax.swing.AbstractAction {",
                "public void actionPerformed(java.awt.event.ActionEvent e) {}",
                "}");
        File clz2 = new File(dir, "clz2");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null, useJsr199());
        assertEquals(Collections.singletonMap("api.MenuItem", Collections.singleton(
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        Map<String,Set<String>> expected = new HashMap<String,Set<String>>();
        expected.put("x.A", new TreeSet<String>(Arrays.asList(new String[] {
            "y.C1{i=33}",
            "y.C2{}",
            "y.C3{}",
        })));
        expected.put("x.B", new TreeSet<String>(Arrays.asList(new String[] {
            "y.C4{}",
            "y.C5{as=[@x.A{}, @x.A{}, @x.A{i=20}]}"
        })));
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", new TreeSet<String>(Arrays.asList(new String[] {
                "y.C#F{b=true}",
                "y.C#m(){b=false}",
        }))), TestUtils.findMetadata(clz));
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
        TestUtils.runApt(src, null, clz, null, null, useJsr199());
        assertEquals(Collections.singletonMap("x.Outer$A", Collections.singleton(
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

}
