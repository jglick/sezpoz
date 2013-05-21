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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Iterator;
import java.util.concurrent.Callable;

import net.java.sezpoz.impl.TestUtils;

import org.junit.Before;
import org.junit.Test;

/**
 * Check class loader issues.
 * 
 * @author Johannes Schindelin
 */
public class ClassLoaderTest {

    private File dir, src1, clz1, src2, clz2;
    private ClassLoader loader1, loader2;
    @Before public void setUp() throws Exception {
        dir = TestUtils.getWorkDir(this);
        TestUtils.clearDir(dir);
        src1 = new File(dir, "src1");
        clz1 = new File(dir, "clz1");
        clz1.mkdirs();
        loader1 = new URLClassLoader(new URL[] {clz1.toURI().toURL()});
        src2 = new File(dir, "src2");
        clz2 = new File(dir, "clz2");
        clz2.mkdirs();
        loader2 = new URLClassLoader(new URL[] {clz2.toURI().toURL()}, loader1);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test public void differentLoaders() throws Exception {
        TestUtils.makeSource(src1, "api.MyAnnotation",
                "import java.lang.annotation.ElementType;",
                "import java.lang.annotation.Retention;",
                "import java.lang.annotation.RetentionPolicy;",
                "import java.lang.annotation.Target;",
                "import java.util.concurrent.Callable;",
                "import net.java.sezpoz.Indexable;",
                "@Retention(RetentionPolicy.RUNTIME)",
                "@Target(ElementType.TYPE)",
                "@Indexable(type = Callable.class)",
                "public @interface MyAnnotation {",
                "Class<?> myClass();",
                "}");
        TestUtils.runApt(src1, null, clz1, new File[0], null);

        TestUtils.makeSource(src2, "com.example.MyExample",
                "import api.MyAnnotation;",
                "import java.util.concurrent.Callable;",
                "@MyAnnotation(myClass = MyExample.class)",
                "public class MyExample implements Callable {",
                "public Object call() {",
                "return \"Hello, World!\";",
                "}",
                "}");
        TestUtils.runApt(src2, null, clz2, new File[] {clz1}, null);

        final Class annotationClass = loader1.loadClass("api.MyAnnotation");
        final Class exampleClass = loader2.loadClass("com.example.MyExample");
        final Index index = Index.load(annotationClass, Callable.class, loader2);

        Iterator<IndexItem> it = index.iterator();
        assertTrue(it.hasNext());
        IndexItem item = it.next();
        assertFalse(it.hasNext());

        final Object annotation = item.annotation();
        final Method myClassMethod = annotation.getClass().getMethod("myClass");
        final Object myClass = myClassMethod.invoke(annotation);
        assertEquals(myClass, exampleClass);
    }

}
