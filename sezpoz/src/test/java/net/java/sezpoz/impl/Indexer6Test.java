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
 * Software is Sun Microsystems, Inc. Portions Copyright 2008 Sun
 * Microsystems, Inc. All Rights Reserved.
 */

package net.java.sezpoz.impl;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.TreeSet;
import static org.junit.Assert.*;
import org.junit.Test;

/**
 * Test for JDK 6 (JSR 199) version of indexer.
 */
public class Indexer6Test extends IndexerTestBase {

    protected boolean useJsr199() {
        return true;
    }

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
        TestUtils.runApt(src1, null, clz1, null, null, useJsr199());
        File src2 = new File(dir, "src2");
        TestUtils.makeSource(src2, "Impl1",
                "@Thing(name=\"one\")",
                "public class Impl1 {}");
        File clz2 = new File(dir, "clz2");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null, useJsr199());
        assertEquals(Collections.singletonMap("Thing", Collections.singleton(
                "Impl1{name=one}"
                )), TestUtils.findMetadata(clz2));
        TestUtils.makeSource(src2, "Impl2",
                "@Thing(name=\"two\")",
                "public class Impl2 {}");
        TestUtils.runApt(src2, "Impl2", clz2, new File[] {clz1}, null, useJsr199());
        assertEquals(Collections.singletonMap("Thing", new TreeSet<String>(Arrays.asList(
                "Impl1{name=one}",
                "Impl2{name=two}"
                ))), TestUtils.findMetadata(clz2));
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
        TestUtils.runApt(src, "A|C1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "public", useJsr199());
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 {@x.A protected static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M4", "public class M4 {@x.A private static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M5", "class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.runApt(src, "A|M1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "public", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M3", clz, null, "public", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M4", clz, null, "public", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M5", clz, null, "public", useJsr199());
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A static final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 {@x.A private static final Object f = null;}");
        TestUtils.makeSource(src, "y.F4", "class F4 {@x.A public static final Object f = null;}");
        TestUtils.runApt(src, "A|F1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "public", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F3", clz, null, "public", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F4", clz, null, "public", useJsr199());
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
        TestUtils.runApt(src, "A|C1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "abstract", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|C3", clz, null, "constructor", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|C4", clz, null, "constructor", useJsr199());
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A public Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 {@x.A public static Object m(int x) {return null;}}");
        TestUtils.runApt(src, "A|M1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "static", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M3", clz, null, "parameters", useJsr199());
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A public final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 {@x.A public static Object f = null;}");
        TestUtils.runApt(src, "A|F1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "static", useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F3", clz, null, "final", useJsr199());
        TestUtils.makeSource(src, "y.N1", "public class N1 {@x.A public static class N {}}");
        TestUtils.makeSource(src, "y.N2", "public class N2 {@x.A public class N {}}");
        TestUtils.runApt(src, "A|N1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|N2", clz, null, "static", useJsr199());
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
        TestUtils.runApt(src, "A|C1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|C2", clz, null, "Runnable", useJsr199());
        TestUtils.makeSource(src, "y.M1", "public class M1 {@x.A public static Runnable m() {return null;}}");
        TestUtils.makeSource(src, "y.M2", "public class M2 {@x.A public static Object m() {return null;}}");
        TestUtils.makeSource(src, "y.M3", "public class M3 implements Runnable {@x.A public static M3 m() {return null;} public void run() {}}");
        TestUtils.runApt(src, "A|M1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|M2", clz, null, "Runnable", useJsr199());
        TestUtils.runApt(src, "A|M3", clz, null, null, useJsr199());
        TestUtils.makeSource(src, "y.F1", "public class F1 {@x.A public static final Runnable f = null;}");
        TestUtils.makeSource(src, "y.F2", "public class F2 {@x.A public static final Object f = null;}");
        TestUtils.makeSource(src, "y.F3", "public class F3 implements Runnable {@x.A public static final F3 f = null; public void run() {}}");
        TestUtils.runApt(src, "A|F1", clz, null, null, useJsr199());
        TestUtils.runAptExpectingErrors(src, "A|F2", clz, null, "Runnable", useJsr199());
        TestUtils.runApt(src, "A|F3", clz, null, null, useJsr199());
    }

    @Test public void inappropriateIndexable() throws Exception {
        TestUtils.makeSource(src, "x.A1",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A1 {}");
        TestUtils.runApt(src, "A1", clz, null, null, useJsr199());
        TestUtils.makeSource(src, "x.A2",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@Inherited",
                "@net.java.sezpoz.Indexable",
                "public @interface A2 {}");
        TestUtils.runAptExpectingErrors(src, "A2", clz, null, "@Inherited", useJsr199());
        TestUtils.makeSource(src, "x.A3",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD, ElementType.CONSTRUCTOR})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A3 {}");
        TestUtils.runAptExpectingErrors(src, "A3", clz, null, "CONSTRUCTOR", useJsr199());
        TestUtils.makeSource(src, "x.A4",
                "@net.java.sezpoz.Indexable",
                "@Retention(RetentionPolicy.SOURCE)",
                "public @interface A4 {}");
        TestUtils.runAptExpectingErrors(src, "A4", clz, null, "@Target", useJsr199());
        TestUtils.makeSource(src, "x.A5",
                "import java.lang.annotation.*;",
                "@Target({})",
                "@Retention(RetentionPolicy.SOURCE)",
                "@net.java.sezpoz.Indexable",
                "public @interface A5 {}");
        TestUtils.runAptExpectingErrors(src, "A5", clz, null, "@Target", useJsr199());
        TestUtils.makeSource(src, "x.A6",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface A6 {}");
        TestUtils.runAptExpectingErrors(src, "A6", clz, null, "@Retention", useJsr199());
    }

}
