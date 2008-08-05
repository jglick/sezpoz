/*
 * The contents of this file are subject to the terms of the Common Development
 * and Distribution License (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at http://www.sun.com/cddl/cddl.html
 * or http://www.netbeans.org/cddl.txt.
 *
 * When distributing Covered Code, include this CDDL Header Notice in each file
 * and include the License file at http://www.netbeans.org/cddl.txt.
 * If applicable, add the following below the CDDL Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * The Original Software is SezPoz. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 2006-2007 Sun
 * Microsystems, Inc. All Rights Reserved.
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
                "@net.java.sezpoz.Indexable(type=javax.swing.Action.class)",
                "public @interface MenuItem {",
                "String menuName();",
                "String itemName();",
                "String iconPath() default \"\";",
                "}");
        File clz1 = new File(dir, "clz1");
        TestUtils.runApt(src1, null, clz1, new File[0], null, useJsr199());
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
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "String x();",
                "}");
        TestUtils.makeSource(src, "y.C",
                "@x.A(x=\"foo\\\\\\\"\\n\")",
                "public class C {}");
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
                "y.C{x=foo\\\"\n}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void compositeVals() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
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
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
                "y.C{b=true, c=x, i=3, other=@x.Other{v=1}, others=[@x.Other{v=2}]}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void defaultValsNotWritten() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A {",
                "int i() default 0;",
                "}");
        TestUtils.makeSource(src, "x.B",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
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
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
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
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
        assertEquals(Collections.singletonMap("x.A", Collections.singleton(
                "y.C{clazz=java.lang.String, enoom=[ONE, TWO]}"
                )), TestUtils.findMetadata(clz));
    }

    @Test public void methodsAndFields() throws Exception {
        TestUtils.makeSource(src, "x.A",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
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
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
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
                "public @interface A {}",
                "}");
        TestUtils.makeSource(src, "y.Auter",
                "public class Auter {",
                "@x.Outer.A",
                "public static class C {}",
                "}");
        TestUtils.runApt(src, null, clz, new File[0], null, useJsr199());
        assertEquals(Collections.singletonMap("x.Outer$A", Collections.singleton(
                "y.Auter$C{}"
                )), TestUtils.findMetadata(clz));
    }

    // XXX to be tested:
    // - errors for improper indexable annotation types (e.g. @Inherited)
    // - errors for improper annotated elements (wrong modifiers, wrong ret type, nonstatic nested class, etc.)
    // - errors reported properly via Messager, not as exceptions

}
