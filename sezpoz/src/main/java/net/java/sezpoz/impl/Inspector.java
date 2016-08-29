package net.java.sezpoz.impl;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/** CLI utility for inspecting binary SezPoz metadata. */
public class Inspector {

    private static final byte[] ZIP_MAGIC = {0x50, 0x4b, 0x03, 0x04};
    private static final byte[] SER_MAGIC = {(byte) 0xac, (byte) 0xed, 0x00, 0x05};

    public static void main(String[] args) throws Exception {
        if (args.length == 0) {
            System.err.println("Usage: java -jar sezpoz.jar [ something.jar | some.serialized.Annotation ]+");
        }
        for (String arg : args) {
            System.out.println("--- " + arg);
            byte[] magic = new byte[4];
            InputStream is = new FileInputStream(arg);
            try {
                is.read(magic, 0, 4);
            } finally {
                is.close();
            }
            if (Arrays.equals(magic, ZIP_MAGIC)) {
                JarFile jf = new JarFile(arg, false);
                try {
                    Enumeration<JarEntry> entries = jf.entries();
                    while (entries.hasMoreElements()) {
                        JarEntry entry = entries.nextElement();
                        String name = entry.getName();
                        if (name.startsWith(Indexer.METAINF_ANNOTATIONS)) {
                            String annotation = name.substring(Indexer.METAINF_ANNOTATIONS.length());
                            if (annotation.isEmpty() || annotation.endsWith(".txt")) {
                                continue;
                            }
                            System.out.println("# " + annotation);
                            is = jf.getInputStream(entry);
                            try {
                                is.read(magic, 0, 4);
                            } finally {
                                is.close();
                            }
                            if ((Arrays.equals(magic, SER_MAGIC))) {
                                is = jf.getInputStream(entry);
                                try {
                                    dump(is);
                                } finally {
                                    is.close();
                                }
                            } else {
                                System.err.println("does not look like a Java serialized file");
                            }
                        }
                    }
                } finally {
                    jf.close();
                }
            } else if (Arrays.equals(magic, SER_MAGIC)) {
                is = new FileInputStream(arg);
                try {
                    dump(is);
                } finally {
                    is.close();
                }
            } else {
                System.err.println("does not look like either a JAR file or a Java serialized file");
            }
        }
    }
    
    private static void dump(InputStream is) throws Exception {
        ObjectInput oi = new ObjectInputStream(is);
        while (true) {
            SerAnnotatedElement el = (SerAnnotatedElement) oi.readObject();
            if (el == null) {
                break;
            }
            System.out.println(el);
        }
    }

}
