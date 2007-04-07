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
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import junit.framework.TestCase;
import net.java.sezpoz.Indexable;

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

    public static File getWorkDir(TestCase test) throws IOException {
        File base = new File(System.getProperty("workdir", System.getProperty("java.io.tmpdir")));
        File workdir = new File(new File(base, test.getClass().getName()), test.getName());
        if (!workdir.isDirectory() && !workdir.mkdirs()) {
            throw new IOException(workdir.getAbsolutePath());
        }
        return workdir;
    }

    /**
     * Run the apt tool.
     * @param src a source root (runs apt on all *.java it finds)
     * @param dest a dest dir (also compiles classes there)
     * @param cp classpath entries for processor (Indexable will always be accessible)
     * @param stderr output stream to use, or null for test console
     */
    public static void runApt(File src, File dest, File[] cp, PrintWriter stderr) throws Exception {
        List<String> args = new ArrayList<String>();
        String indexableLoc = new File(URI.create(Indexable.class.getProtectionDomain().getCodeSource().getLocation().toExternalForm())).getAbsolutePath();
        args.add("-factorypath");
        args.add(indexableLoc);
        args.add("-classpath");
        StringBuffer b = new StringBuffer(indexableLoc);
        for (File entry : cp) {
            b.append(File.pathSeparatorChar);
            b.append(entry.getAbsolutePath());
        }
        args.add(b.toString());
        args.add("-d");
        args.add(dest.getAbsolutePath());
        dest.mkdirs();
        scan(args, src);
        if (stderr == null) {
            stderr = new PrintWriter(System.err);
        }
        //System.err.println("running apt with args: " + args);
        int res = com.sun.tools.apt.Main.process(stderr, args.toArray(new String[args.size()]));
        if (res != 0) {
            throw new Exception("nonzero exit code: " + res);
        }
    }
    private static void scan(List<String> names, File f) {
        if (f.isDirectory()) {
            for (File kid : f.listFiles()) {
                scan(names, kid);
            }
        } else if (f.getName().endsWith(".java")) {
            names.add(f.getAbsolutePath());
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
        Map<String,SortedSet<String>> metadata = new HashMap<String,SortedSet<String>>();
        File dir = new File(new File(dest, "META-INF"), "annotations");
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
