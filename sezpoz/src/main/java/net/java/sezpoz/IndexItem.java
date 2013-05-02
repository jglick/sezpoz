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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.java.sezpoz.impl.SerAnnConst;
import net.java.sezpoz.impl.SerAnnotatedElement;
import net.java.sezpoz.impl.SerEnumConst;
import net.java.sezpoz.impl.SerTypeConst;

/**
 * One index item.
 * May be associated with a class, method, or field.
 * Caches result of {@link #element} and {@link #instance} after first call.
 * Not thread-safe.
 * @param A the type of annotation being loaded
 * @param I the type of instance being loaded
 */
public final class IndexItem<A extends Annotation,I> {

    private static final Logger LOGGER = Logger.getLogger(IndexItem.class.getName());

    private final SerAnnotatedElement structure;
    private final Class<A> annotationType;
    private final Class<I> instanceType;
    private final ClassLoader loader;
    private final URL resource;
    private AnnotatedElement element;
    private Object instance;

    IndexItem(SerAnnotatedElement structure, Class<A> annotationType, Class<I> instanceType, ClassLoader loader, URL resource) throws IOException {
        this.structure = structure;
        this.annotationType = annotationType;
        this.instanceType = instanceType;
        this.loader = loader;
        this.resource = resource;
        LOGGER.log(Level.FINE, "Loaded index item {0}", structure);
    }

    /**
     * Get the annotation itself.
     * A lightweight proxy will be returned which obeys the
     * {@link Annotation} contract and should be equal to (but not identical
     * to) the "real" annotation available from {@link AnnotatedElement#getAnnotation}
     * on {@link #element}
     * (if in fact it has runtime retention, which is encouraged but not required).
     * @return a live or proxy annotation
     */
    public A annotation() {
        return proxy(loader, annotationType, structure.values);
    }

    /**
     * Determine what kind of element is annotated.
     * @return one of {@link ElementType#TYPE}, {@link ElementType#METHOD}, or {@link ElementType#FIELD}
     */
    public ElementType kind() {
        return structure.isMethod ? ElementType.METHOD : structure.memberName != null ? ElementType.FIELD : ElementType.TYPE;
    }

    /**
     * Get the name of the class which is the annotated element or of which the annotated element is a member.
     * @return the class name (format e.g. "x.y.Z$I")
     */
    public String className() {
        return structure.className;
    }

    /**
     * Get the name of the annotated member element.
     * @return a method or field name, or null if the annotated element is a class
     */
    public String memberName() {
        return structure.memberName;
    }

    /**
     * Get the live annotated element.
     * @return a {@link Class}, {@link Method}, or {@link Field}
     * @throws InstantiationException if the class cannot be loaded or there is some other reflective problem
     */
    public AnnotatedElement element() throws InstantiationException {
        if (element == null) {
            try {
                Class<?> impl = loader.loadClass(className());
                if (structure.isMethod) {
                    element = impl.getMethod(structure.memberName);
                } else if (structure.memberName != null) {
                    element = impl.getField(structure.memberName);
                } else {
                    element = impl;
                }
                LOGGER.log(Level.FINER, "Loaded annotated element: {0}", element);
            } catch (Exception x) {
                throw (InstantiationException) new InstantiationException(labelFor(resource) + " might need to be rebuilt: " + x).initCause(x);
            } catch (LinkageError x) {
                throw (InstantiationException) new InstantiationException(x.toString()).initCause(x);
            }
        }
        return element;
    }

    private static String labelFor(URL resource) {
        String u = resource.toString();
        Matcher m = Pattern.compile("jar:(file:.+)!/.+").matcher(u);
        if (m.matches()) {
            return new File(URI.create(m.group(1))).getAbsolutePath();
        } else {
            return u;
        }
    }

    /**
     * Get an instance referred to by the element.
     * This instance is cached by the item object.
     * The element must always be public.
     * <ol>
     * <li>In case of a class, the class will be instantiated by a public no-argument constructor.
     * <li>In case of a method, it must be static and have no arguments; it will be called.
     * <li>In case of a field, it must be static and final; its value will be used.
     * </ol>
     * @return an object guaranteed to be assignable to the {@link Indexable#type} if specified
     *         (or may be null, in the case of a method or field)
     * @throws InstantiationException for the same reasons as {@link #element},
     *                                or if creating the object fails
     */
    public I instance() throws InstantiationException {
        if (instance == null) {
            AnnotatedElement e = element();
            try {
                if (e instanceof Class<?>) {
                    instance = ((Class<?>) e).newInstance();
                } else if (e instanceof Method) {
                    instance = ((Method) e).invoke(null);
                } else {
                    instance = ((Field) e).get(null);
                }
                LOGGER.log(Level.FINER, "Loaded instance: {0}", instance);
            } catch (InstantiationException x) {
                throw x;
            } catch (Exception x) {
                throw (InstantiationException) new InstantiationException(x.toString()).initCause(x);
            } catch (LinkageError x) {
                throw (InstantiationException) new InstantiationException(x.toString()).initCause(x);
            }
        }
        return instanceType.cast(instance);
    }

    @Override
    public int hashCode() {
        return className().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof IndexItem<?,?>)) {
            return false;
        }
        IndexItem<? extends Annotation,?> o = (IndexItem<?,?>) obj;
        return structure.equals(o.structure) && annotationType == o.annotationType && loader == o.loader;
    }

    @Override
    public String toString() {
        return "@" + annotationType.getName() + ":" + structure;
    }

    private static <T extends Annotation> T proxy(ClassLoader loader, Class<T> type, Map<String,Object> data) {
        return type.cast(Proxy.newProxyInstance(loader,
                new Class<?>[] {type},
                new AnnotationProxy(loader, type, data)));
    }

    /**
     * Manages a proxy for the live annotation.
     */
    private static final class AnnotationProxy implements InvocationHandler {

        /** class loader to use */
        private final ClassLoader loader;
        /** type of the annotation */
        private final Class<? extends Annotation> type;
        /** (non-default) annotation method values; value may be wrapped in Ser*Const objects or ArrayList */
        private final Map<String,Object> data;

        public AnnotationProxy(ClassLoader loader, Class<? extends Annotation> type, Map<String,Object> data) {
            this.loader = loader;
            this.type = type;
            this.data = data;
        }

        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            String name = method.getName();
            if (name.equals("annotationType") && method.getParameterTypes().length == 0) {
                return type;
            } else if (name.equals("hashCode") && method.getParameterTypes().length == 0) {
                // See Annotation#hashCode for explanation of algorithm.
                int x = 0;
                for (Method m : type.getDeclaredMethods()) {
                    Object val = annCall(m);
                    int valhash = val.hashCode();
                    Class<?> arrClazz;
                    if (val instanceof Object[]) {
                        arrClazz = Object[].class;
                    } else {
                        arrClazz = val.getClass();
                    }
                    try {
                        Method arraysHashCode = Arrays.class.getMethod("hashCode", arrClazz);
                        valhash = (Integer) arraysHashCode.invoke(null, val);
                    } catch (NoSuchMethodException nsme) {
                        // fine, not an array object
                    }
                    x += (127 * m.getName().hashCode()) ^ valhash;
                }
                return x;
            } else if (name.equals("equals") && method.getParameterTypes().length == 1 && method.getParameterTypes()[0] == Object.class) {
                // All annotation values have to be equal (even if defaulted).
                if (!(args[0] instanceof Annotation)) {
                    return false;
                }
                Annotation o = (Annotation) args[0];
                if (type != o.annotationType()) {
                    return false;
                }
                for (Method m : type.getDeclaredMethods()) {
                    Object myval = annCall(m);
                    Object other = m.invoke(o);
                    Class<?> arrClazz;
                    if (myval instanceof Object[]) {
                        arrClazz = Object[].class;
                    } else {
                        arrClazz = myval.getClass();
                    }
                    try {
                        Method arraysEquals = Arrays.class.getMethod("equals", arrClazz, arrClazz);
                        if (!((Boolean) arraysEquals.invoke(null, myval, other))) {
                            return false;
                        }
                    } catch (NoSuchMethodException nsme) {
                        // fine, not an array object
                        if (!myval.equals(other)) {
                            return false;
                        }
                    }
                }
                return true;
            } else if (name.equals("toString") && method.getParameterTypes().length == 0) {
                // No firm contract, just for debugging.
                return "@" + type.getName() + data;
            } else {
                // Anything else is presumed to be one of the annotation methods.
                return annCall(method);
            }
        }

        /**
         * Invoke an annotation method.
         */
        private Object annCall(Method m) throws Exception {
            assert m.getParameterTypes().length == 0;
            String name = m.getName();
            if (data.containsKey(name)) {
                return evaluate(data.get(name), m.getReturnType());
            } else {
                Object o = m.getDefaultValue();
                assert o != null;
                return o;
            }
        }

        /**
         * Unwrap a value to a live type.
         */
        private Object evaluate(Object o, Class<?> expectedType) throws Exception {
            if (o instanceof SerAnnConst) {
                SerAnnConst a = (SerAnnConst) o;
                return proxy(loader, loader.loadClass(a.name).asSubclass(Annotation.class), a.values);
            } else if (o instanceof SerTypeConst) {
                return loader.loadClass(((SerTypeConst) o).name);
            } else if (o instanceof SerEnumConst) {
                SerEnumConst e = (SerEnumConst) o;
                return loader.loadClass(e.enumName).getField(e.constName).get(null);
            } else if (o instanceof ArrayList<?>) {
                List<?> l = (List<?>) o;
                Class<?> compType = expectedType.getComponentType();
                int size = l.size();
                Object arr = Array.newInstance(compType, size);
                for (int i = 0; i < size; i++) {
                    Array.set(arr, i, evaluate(l.get(i), compType));
                }
                return arr;
            } else {
                return o;
            }
        }

    }

}
