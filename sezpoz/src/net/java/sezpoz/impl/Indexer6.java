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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import net.java.sezpoz.Indexable;

/**
 * Processor for indexable annotations.
 */
@SupportedSourceVersion(SourceVersion.RELEASE_6)
@SupportedAnnotationTypes("*")
public class Indexer6 extends AbstractProcessor {

    /** public for ServiceLoader */
    public Indexer6() {}

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            return false;
        }
        // map from indexable annotation names, to actual uses
        Map<String,List<SerAnnotatedElement>> output = new HashMap<String,List<SerAnnotatedElement>>();
        for (TypeElement ann : annotations) {
            AnnotationMirror indexable = null;
            for (AnnotationMirror _indexable : processingEnv.getElementUtils().getAllAnnotationMirrors(ann)) {
                if (processingEnv.getElementUtils().getBinaryName((TypeElement) _indexable.getAnnotationType().asElement()).
                        contentEquals(Indexable.class.getName())) {
                    indexable = _indexable;
                    break;
                }
            }
            if (indexable == null) {
                continue;
            }
            String annName = processingEnv.getElementUtils().getBinaryName(ann).toString();
            for (Element elt : roundEnv.getElementsAnnotatedWith(ann)) {
                // XXX check that it is not @Inherited, and that it has the right @Target
                // XXX check that decl is public
                // XXX check that decl is static if a method, etc.
                // XXX check that decl is assignable to objType if that is not null
                List<SerAnnotatedElement> existingOutput = output.get(annName);
                if (existingOutput == null) {
                    existingOutput = new ArrayList<SerAnnotatedElement>();
                    output.put(annName, existingOutput);
                }
                existingOutput.add(makeSerAnnotatedElement(elt, ann));
            }
        }
        for (Map.Entry<String, List<SerAnnotatedElement>> outputEntry : output.entrySet()) {
            String annName = outputEntry.getKey();
            try {
                List<SerAnnotatedElement> elements = outputEntry.getValue();
                try {
                    FileObject in = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/annotations/" + annName);
                    // Read existing annotations, for incremental compilation.
                    InputStream is = in.openInputStream();
                    try {
                        ObjectInputStream ois = new ObjectInputStream(is);
                        while (true) {
                            SerAnnotatedElement el;
                            try {
                                el = (SerAnnotatedElement) ois.readObject();
                            } catch (ClassNotFoundException cnfe) {
                                throw new IOException(cnfe.toString());
                            }
                            if (el == null) {
                                break;
                            }
                            elements.add(el);
                        }
                    } finally {
                        is.close();
                    }
                } catch (FileNotFoundException x) {
                    // OK, created for the first time
                }
                FileObject out = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", "META-INF/annotations/" + annName/*, XXX originatingElements */);
                OutputStream os = out.openOutputStream();
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    for (SerAnnotatedElement el : elements) {
                        oos.writeObject(el);
                    }
                    oos.writeObject(null);
                    oos.flush();
                } finally {
                    os.close();
                }
            } catch (IOException x) {
                processingEnv.getMessager().printMessage(Kind.ERROR, x.toString());
            }
        }
        return false;
    }

    private SerAnnotatedElement makeSerAnnotatedElement(Element elt, TypeElement ann) {
        String className, memberName;
        boolean isMethod;
        if (elt instanceof TypeElement) {
            className = processingEnv.getElementUtils().getBinaryName((TypeElement) elt).toString();
            memberName = null;
            isMethod = false;
        } else {
            className = processingEnv.getElementUtils().getBinaryName((TypeElement) elt.getEnclosingElement()).toString();
            memberName = elt.getSimpleName().toString();
            isMethod = elt instanceof ExecutableElement;
        }
        return new SerAnnotatedElement(className, memberName, isMethod, translate(elt.getAnnotationMirrors(), ann));
    }

    private TreeMap<String,Object> translate(List<? extends AnnotationMirror> mirrors, TypeElement ann) {
        TreeMap<String,Object> values = new TreeMap<String,Object>();
        for (AnnotationMirror mirror : mirrors) {
            if (processingEnv.getTypeUtils().isSameType(mirror.getAnnotationType(), ann.asType())) {
                for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> entry : mirror.getElementValues().entrySet()) {
                    values.put(entry.getKey().getSimpleName().toString(), translate(entry.getValue().getValue()));
                }
            }
        }
        return values;
    }

    private Object translate(Object annval) {
        if (annval instanceof List) {
            @SuppressWarnings("unchecked")
            List<? extends AnnotationValue> annvals = (List) annval;
            List<Object> values = new ArrayList<Object>(annvals.size());
            for (AnnotationValue v : annvals) {
                values.add(translate(v.getValue()));
            }
            return values;
        } else if (annval instanceof TypeMirror) {
            return new SerTypeConst(((TypeMirror) annval).toString()); // XXX is TypeMirror.toString safe enough?
        } else if (annval instanceof VariableElement) {
            VariableElement elt = (VariableElement) annval;
            return new SerEnumConst(processingEnv.getElementUtils().getBinaryName(
                    (TypeElement) elt.getEnclosingElement()).toString(), elt.getSimpleName().toString());
        } else if (annval instanceof AnnotationMirror) {
            AnnotationMirror am = (AnnotationMirror) annval;
            TreeMap<String,Object> values = new TreeMap<String,Object>();
            for (Map.Entry<? extends ExecutableElement,? extends AnnotationValue> entry : am.getElementValues().entrySet()) {
                values.put(entry.getKey().getSimpleName().toString(), translate(entry.getValue().getValue()));
            }
            return new SerAnnConst(am.getAnnotationType().toString(), values);// XXX or use asElement?
        } else {
            return annval;
        }
    }

}
