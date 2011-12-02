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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.regex.Pattern;
import javax.tools.ToolProvider;
import net.java.sezpoz.Indexable;
import org.junit.Assert;

/**
 * Common utilities used during testing.
 */
public class TestUtils {
    
    private TestUtils() {}
    
    public static void clearDir(File d) throws IOException {
        File[] kids = d.listFiles();
        if (kids != null) {
            for (File kid : kids) {
                if (kid.isDirectory()) {
                    clearDir(kid);
                }
                if (!kid.delete()) {
                    throw new IOException(kid.getAbsolutePath());
                }
            }
        }
    }

    private static Map<String,Integer> counts = new HashMap<String,Integer>();
    public static File getWorkDir(Object test) throws IOException {
        File base = new File(System.getProperty("workdir", System.getProperty("java.io.tmpdir")));
        String name = test.getClass().getName();
        Integer count = counts.get(name);
        if (count == null) {
            count = 0;
        }
        counts.put(name, ++count);
        File workdir = new File(new File(base, name), "test" + count);
        if (!workdir.isDirectory() && !workdir.mkdirs()) {
            throw new IOException(workdir.getAbsolutePath());
        }
        return workdir;
    }

    /**
     * Run the apt tool.
     * @param src a source root (runs apt on all *.java it finds)
     * @param srcIncludes optional regex to limit class names to compile
     * @param dest a dest dir (also compiles classes there)
     * @param cp classpath entries for processor (Indexable will always be accessible), or null
     * @param stderr output stream to use, or null for test console
     * @param jsr199 whether to use JSR 199 (i.e. JDK 6 javac) annotation processing
     * @throws AptFailedException if processing failed
     * @throws Exception if something unexpected went wrong
     */
    public static void runApt(File src, String srcIncludes, File dest, File[] cp, OutputStream stderr, boolean jsr199) throws Exception {
        List<String> args = new ArrayList<String>();
        String indexableLoc = new File(URI.create(Indexable.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm())).getAbsolutePath();
        args.add(jsr199 ? "-processorpath" : "-factorypath");
        args.add(indexableLoc);
        args.add("-classpath");
        StringBuffer b = new StringBuffer(indexableLoc);
        if (cp != null) {
            for (File entry : cp) {
                b.append(File.pathSeparatorChar);
                b.append(entry.getAbsolutePath());
            }
        }
        args.add(b.toString());
        args.add("-d");
        args.add(dest.getAbsolutePath());
        dest.mkdirs();
        args.add("-Asezpoz.quiet=true");
        scan(args, src, srcIncludes);
        if (!jsr199) {
            args.add("-proc:none");
        }
        //System.err.println("running apt with args: " + args);
        String[] argsA = args.toArray(new String[args.size()]);
        int res;
        if (jsr199) {
            res = ToolProvider.getSystemJavaCompiler().run(null, null, stderr, argsA);
            //res = com.sun.tools.javac.Main.compile(argsA, stderr);
        } else {
            res = com.sun.tools.apt.Main.process(new PrintWriter(stderr != null ? stderr : System.err), argsA);
        }
        if (res != 0) {
            throw new AptFailedException(res);
        }
    }
    private static void scan(List<String> names, File f, String includes) {
        if (f.isDirectory()) {
            for (File kid : f.listFiles()) {
                scan(names, kid, includes);
            }
        } else if (f.getName().endsWith(".java") && (includes == null || Pattern.compile(includes).matcher(f.getName()).find())) {
            names.add(f.getAbsolutePath());
        }
    }

    /**
     * Run the apt tool and expect an error to be issued.
     * @param src a source root (runs apt on all *.java it finds)
     * @param srcIncludes optional regex to limit class names to compile
     * @param dest a dest dir (also compiles classes there)
     * @param cp classpath entries for processor (Indexable will always be accessible), or null
     * @param error an error you expect to see printed (APT must also fail), else assertion failure
     * @param jsr199 whether to use JSR 199 (i.e. JDK 6 javac) annotation processing
     * @throws Exception if something unexpected went wrong
     */
    public static void runAptExpectingErrors(File src, String srcIncludes, File dest, File[] cp, String error, boolean jsr199) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            runApt(src, srcIncludes, dest, cp, baos, jsr199);
            Assert.fail("annotation processing should have failed");
        } catch (AptFailedException x) {
            String log = baos.toString();
            Assert.assertTrue(log, log.contains(error));
        }
    }
    
    /**
     * Create a source file.
     * @param dir source root
     * @param clazz a class name
     * @param content lines of text (skip package decl)
     */
    public static void makeSource(File dir, String clazz, String... content) throws IOException {
        File f = new File(dir, clazz.replace('.', File.separatorChar) + ".java");
        f.getParentFile().mkdirs();
        Writer w = new FileWriter(f);
        try {
            PrintWriter pw = new PrintWriter(w);
            String pkg = clazz.replaceFirst("\\.[^.]+$", "");
            if (!pkg.equals(clazz)) {
                pw.println("package " + pkg + ";");
            }
            for (String line : content) {
                pw.println(line);
            }
            pw.flush();
        } finally {
            w.close();
        }
    }
    
    /**
     * Find contents of META-INF/annotations/* in a dest dir.
     * @return map from simple file names to set of SerAnnotatedElement.toString()s
     */
    public static Map<String,SortedSet<String>> findMetadata(File dest) throws Exception {
        File dir = new File(new File(dest, "META-INF"), "annotations");
        if (!dir.isDirectory()) {
            return Collections.emptyMap();
        }
        Map<String,SortedSet<String>> metadata = new HashMap<String,SortedSet<String>>();
        for (String kid : dir.list()) {
            File f = new File(dir, kid);
            InputStream is = new FileInputStream(f);
            try {
                ObjectInputStream ois = new ObjectInputStream(is);
                SortedSet<String> entries = new TreeSet<String>();
                while (true) {
                    SerAnnotatedElement el = (SerAnnotatedElement) ois.readObject();
                    if (el == null) {
                        break;
                    }
                    entries.add(el.toString());
                }
                metadata.put(kid, entries);
            } finally {
                is.close();
            }
        }
        return metadata;
    }
    
}
