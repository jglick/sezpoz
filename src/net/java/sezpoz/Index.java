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

package net.java.sezpoz;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.java.sezpoz.impl.SerAnnotatedElement;

/**
 * Represents an index of a single annotation.
 * Indices are <em>not</em> automatically cached
 * (but reading them should be pretty cheap anyway).
 * @param T the type of annotation to load
 */
public final class Index<T extends Annotation> implements Iterable<IndexItem<T>> {

    /**
     * Load an index for a given annotation type.
     * Uses the thread's context class loader to find the index and load annotated classes.
     * @param annotation the type of annotation to find
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     */
    public static <T extends Annotation> Index<T> load(Class<T> annotation) throws IllegalArgumentException {
        return load(annotation, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Load an index for a given annotation type.
     * @param annotation the type of annotation to find
     * @param loader a class loader in which to find the index and any annotated classes
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     */
    public static <T extends Annotation> Index<T> load(Class<T> annotation, ClassLoader loader) throws IllegalArgumentException {
        return new Index<T>(annotation, loader);
    }

    private final Class<T> annotation;
    private final ClassLoader loader;

    private Index(Class<T> annotation, ClassLoader loader) {
        this.annotation = annotation;
        this.loader = loader;
    }

    /**
     * Find all items in the index.
     * Calls to iterator methods may fail with {@link IndexError}
     * as the index is parsed lazily.
     * @return an iterator over items in the index
     */
    public Iterator<IndexItem<T>> iterator() {
        return new LazyIndexIterator();
    }

    /**
     * Lazy iterator. Opens and parses annotation streams only on demand.
     */
    private final class LazyIndexIterator implements Iterator<IndexItem<T>> {

        private Enumeration<URL> resources;
        private ObjectInputStream ois;
        private IndexItem<T> next;
        private boolean end;
        private final Set<String> loadedMembers = new HashSet<String>();

        public LazyIndexIterator() {}

        private void peek() throws IndexError {
            try {
                doPeek();
            } catch (Exception x) {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException x2) {
                        Logger.getLogger(Index.class.getName()).log(Level.WARNING, null, x2);
                    }
                }
                throw new IndexError(x);
            }
        }

        private void doPeek() throws Exception {
            if (next != null || end) {
                return;
            }
            if (ois == null) {
                if (resources == null) {
                    resources = loader.getResources("META-INF/annotations/" + annotation.getName());
                }
                if (!resources.hasMoreElements()) {
                    // Exhausted all streams.
                    end = true;
                    return;
                }
                ois = new ObjectInputStream(resources.nextElement().openStream());
            }
            SerAnnotatedElement el = (SerAnnotatedElement) ois.readObject();
            if (el == null) {
                // Skip to next stream.
                ois.close();
                ois = null;
                doPeek();
                return;
            }
            String memberName = el.isMethod ? el.className + '#' + el.memberName + "()" :
                el.memberName != null ? el.className + '#' + el.memberName :
                    el.className;
            if (!loadedMembers.add(memberName)) {
                // Already encountered this element, so skip it.
                doPeek();
                return;
            }
            next = new IndexItem<T>(el, annotation, loader);
        }

        public boolean hasNext() {
            peek();
            return !end;
        }

        public IndexItem<T> next() {
            peek();
            if (!end) {
                assert next != null;
                IndexItem<T> _next = next;
                next = null;
                return _next;
            } else {
                throw new NoSuchElementException();
            }
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

}
