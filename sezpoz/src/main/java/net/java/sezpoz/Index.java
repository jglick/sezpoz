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

import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Arrays;
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
 * @param A the type of annotation to load
 * @param I the type of instance which will be created
 */
public final class Index<A extends Annotation,I> implements Iterable<IndexItem<A,I>> {

    private static final Logger LOGGER = Logger.getLogger(Index.class.getName());

    /**
     * Load an index for a given annotation type.
     * Uses the thread's context class loader to find the index and load annotated classes.
     * @param annotation the type of annotation to find
     * @param instanceType the type of instance to be created (use {@link Void} if all instances will be null)
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     *                                  or the instance type is not equal to or a supertype of the annotation's actual {@link Indexable#type}
     */
    public static <A extends Annotation,I> Index<A,I> load(Class<A> annotation, Class<I> instanceType) throws IllegalArgumentException {
        return load(annotation, instanceType, Thread.currentThread().getContextClassLoader());
    }

    /**
     * Load an index for a given annotation type.
     * @param annotation the type of annotation to find
     * @param instanceType the type of instance to be created (use {@link Void} if all instances will be null)
     * @param loader a class loader in which to find the index and any annotated classes
     * @return an index of all elements known to be annotated with it
     * @throws IllegalArgumentException if the annotation type is not marked with {@link Indexable}
     *                                  or the instance type is not equal to or a supertype of the annotation's actual {@link Indexable#type}
     */
    public static <A extends Annotation,I> Index<A,I> load(Class<A> annotation, Class<I> instanceType, ClassLoader loader) throws IllegalArgumentException {
        return new Index<A,I>(annotation, instanceType, loader);
    }

    private final Class<A> annotation;
    private final Class<I> instanceType;
    private final ClassLoader loader;

    private Index(Class<A> annotation, Class<I> instance, ClassLoader loader) {
        this.annotation = annotation;
        this.instanceType = instance;
        this.loader = loader;
    }

    /**
     * Find all items in the index.
     * Calls to iterator methods may fail with {@link IndexError}
     * as the index is parsed lazily.
     * @return an iterator over items in the index
     */
    public Iterator<IndexItem<A,I>> iterator() {
        return new LazyIndexIterator();
    }

    /**
     * Lazy iterator. Opens and parses annotation streams only on demand.
     */
    private final class LazyIndexIterator implements Iterator<IndexItem<A,I>> {

        private Enumeration<URL> resources;
        private ObjectInputStream ois;
        private URL resource;
        private IndexItem<A,I> next;
        private boolean end;
        private final Set<String> loadedMembers = new HashSet<String>();

        public LazyIndexIterator() {
            if (LOGGER.isLoggable(Level.FINE)) {
                String urls;
                if (loader instanceof URLClassLoader) {
                    urls = " " + Arrays.toString(((URLClassLoader) loader).getURLs());
                } else {
                    urls = "";
                }
                LOGGER.log(Level.FINE, "Searching for indices of {0} in {1}{2}", new Object[] {annotation, loader, urls});
            }
        }

        private void peek() throws IndexError {
            try {
                for (int iteration = 0; true; iteration++) {
                    if (iteration == 9999) {
                        LOGGER.log(Level.WARNING, "possible endless loop getting index for {0} from {1}", new Object[] {annotation, loader});
                    }
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
                        resource = resources.nextElement();
                        LOGGER.log(Level.FINE, "Loading index from {0}", resource);
                        ois = new ObjectInputStream(resource.openStream());
                    }
                    SerAnnotatedElement el = (SerAnnotatedElement) ois.readObject();
                    if (el == null) {
                        // Skip to next stream.
                        ois.close();
                        ois = null;
                        continue;
                    }
                    String memberName = el.isMethod ? el.className + '#' + el.memberName + "()" :
                        el.memberName != null ? el.className + '#' + el.memberName :
                            el.className;
                    if (!loadedMembers.add(memberName)) {
                        // Already encountered this element, so skip it.
                        LOGGER.log(Level.FINE, "Already loaded index item {0}", el);
                        continue;
                    }
                    // XXX JRE #6865375 would make loader param accurate for duplicated modules
                    next = new IndexItem<A,I>(el, annotation, instanceType, loader, resource);
                    break;
                }
            } catch (Exception x) {
                if (ois != null) {
                    try {
                        ois.close();
                    } catch (IOException x2) {
                        LOGGER.log(Level.WARNING, null, x2);
                    }
                }
                throw new IndexError(x);
            }
        }

        public boolean hasNext() {
            peek();
            return !end;
        }

        public IndexItem<A,I> next() {
            peek();
            if (!end) {
                assert next != null;
                IndexItem<A,I> _next = next;
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
