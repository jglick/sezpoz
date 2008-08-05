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

/**
 * Test for JDK 6 (JSR 199) version of indexer.
 */
public class Indexer6Test extends IndexerTestBase {

    public Indexer6Test(String name) {
        super(name);
    }

    @Override
    protected boolean useJsr199() {
        return true;
    }

    public void testIncrementalCompilation() throws Exception {
        File src1 = new File(dir, "src1");
        TestUtils.makeSource(src1, "Thing",
                "import java.lang.annotation.*;",
                "@Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})",
                "@net.java.sezpoz.Indexable",
                "public @interface Thing {",
                "String name();",
                "}");
        File clz1 = new File(dir, "clz1");
        TestUtils.runApt(src1, null, clz1, new File[0], null, useJsr199());
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

}
