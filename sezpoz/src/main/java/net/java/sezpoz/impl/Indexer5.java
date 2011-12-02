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

import com.sun.mirror.apt.AnnotationProcessor;
import com.sun.mirror.apt.AnnotationProcessorEnvironment;
import com.sun.mirror.apt.Filer;
import com.sun.mirror.declaration.AnnotationMirror;
import com.sun.mirror.declaration.AnnotationTypeDeclaration;
import com.sun.mirror.declaration.AnnotationTypeElementDeclaration;
import com.sun.mirror.declaration.AnnotationValue;
import com.sun.mirror.declaration.Declaration;
import com.sun.mirror.declaration.EnumConstantDeclaration;
import com.sun.mirror.declaration.FieldDeclaration;
import com.sun.mirror.declaration.MethodDeclaration;
import com.sun.mirror.declaration.TypeDeclaration;
import com.sun.mirror.type.DeclaredType;
import com.sun.mirror.type.TypeMirror;
import java.io.File;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import net.java.sezpoz.Indexable;

/**
 * Processor for indexable annotations.
 */
class Indexer5 implements AnnotationProcessor {

    private final AnnotationProcessorEnvironment env;
    /** map from indexable annotation names, to actual uses */
    private final Map<String,List<SerAnnotatedElement>> output;

    public Indexer5(Set<AnnotationTypeDeclaration> ignore, AnnotationProcessorEnvironment env) {
        this.env = env;
        output = new HashMap<String,List<SerAnnotatedElement>>();
    }

    public void process() {
        // XXX how does getSpecifiedTypeDeclarations differ?
        for (TypeDeclaration decl : env.getTypeDeclarations()) {
            analyze(decl);
            for (MethodDeclaration m : decl.getMethods()) {
                analyze(m);
            }
            for (FieldDeclaration f : decl.getFields()) {
                analyze(f);
            }
        }
        for (Map.Entry<String,List<SerAnnotatedElement>> outputEntry : output.entrySet()) {
            String annName = outputEntry.getKey();
            try {
                OutputStream os = env.getFiler().createBinaryFile(Filer.Location.CLASS_TREE, "",
                        new File("META-INF" + File.separator + "annotations" + File.separator + annName));
                try {
                    ObjectOutputStream oos = new ObjectOutputStream(os);
                    for (SerAnnotatedElement el : outputEntry.getValue()) {
                        oos.writeObject(el);
                    }
                    oos.writeObject(null);
                    oos.flush();
                } finally {
                    os.close();
                }
            } catch (IOException x) {
                env.getMessager().printError(x.toString());
            }
        }
    }

    private void analyze(Declaration decl) {
        for (AnnotationMirror ann : decl.getAnnotationMirrors()) {
            // First check to see if it is an @Indexable annotation.
            boolean indexable = false;
            TypeMirror objType = null; // upper type bound on annotated elements, or null for Object.class
            for (AnnotationMirror metaAnn : ann.getAnnotationType().getDeclaration().getAnnotationMirrors()) {
                if (getQualifiedNameUsingShell(metaAnn.getAnnotationType().getDeclaration()).equals(Indexable.class.getName())) {
                    // Yes it is. Now just find the type=... if there is one:
                    Map<AnnotationTypeElementDeclaration,AnnotationValue> values = metaAnn.getElementValues();
                    if (!values.isEmpty()) {
                        AnnotationValue val = values.values().iterator().next();
                        objType = (TypeMirror) val.getValue();
                    }
                    indexable = true;
                    break;
                }
            }
            if (!indexable) {
                continue;
            }
            // XXX check that it is not @Inherited, and that it has the right @Target
            // XXX check that decl is public
            // XXX check that decl is static if a method, etc.
            // XXX check that decl is assignable to objType if that is not null
            String annName = getQualifiedNameUsingShell(ann.getAnnotationType().getDeclaration());
            List<SerAnnotatedElement> existingOutput = output.get(annName);
            if (existingOutput == null) {
                existingOutput = new ArrayList<SerAnnotatedElement>();
                output.put(annName, existingOutput);
            }
            existingOutput.add(makeSerAnnotatedElement(decl, ann.getElementValues()));
        }
    }

    /**
     * Similar to {@link TypeDeclaration#getQualifiedName} but uses p.O$I rather than p.O.I.
     * In JDK 6 could use Elements.getBinaryName.
     */
    static String getQualifiedNameUsingShell(TypeDeclaration decl) {
        TypeDeclaration outer = decl.getDeclaringType();
        if (outer != null) {
            return getQualifiedNameUsingShell(outer) + '$' + decl.getSimpleName();
        } else {
            return decl.getQualifiedName();
        }
    }

    private SerAnnotatedElement makeSerAnnotatedElement(Declaration decl, Map<AnnotationTypeElementDeclaration,AnnotationValue> annvalues) {
        String className, memberName;
        boolean isMethod;
        if (decl instanceof TypeDeclaration) {
            className = getQualifiedNameUsingShell((TypeDeclaration) decl);
            memberName = null;
            isMethod = false;
        } else if (decl instanceof MethodDeclaration) {
            MethodDeclaration _decl = (MethodDeclaration) decl;
            className = getQualifiedNameUsingShell(_decl.getDeclaringType());
            memberName = _decl.getSimpleName();
            isMethod = true;
        } else {
            FieldDeclaration _decl = (FieldDeclaration) decl;
            className = getQualifiedNameUsingShell(_decl.getDeclaringType());
            memberName = _decl.getSimpleName();
            isMethod = false;
        }
        return new SerAnnotatedElement(className, memberName, isMethod, translate(annvalues));
    }

    private static TreeMap<String,Object> translate(Map<AnnotationTypeElementDeclaration,AnnotationValue> annvalues) {
        TreeMap<String,Object> values = new TreeMap<String,Object>();
        for (Map.Entry<AnnotationTypeElementDeclaration,AnnotationValue> entry : annvalues.entrySet()) {
            String key = entry.getKey().getSimpleName();
            AnnotationValue val = entry.getValue();
            values.put(key, translate(val.getValue()));
        }
        return values;
    }

    private static Object translate(Object annval) {
        if (annval instanceof Collection) {
            @SuppressWarnings("unchecked")
            Collection<AnnotationValue> annvals = (Collection) annval;
            List<Object> values = new ArrayList<Object>(annvals.size());
            for (AnnotationValue v : annvals) {
                values.add(translate(v.getValue()));
            }
            return values;
        } else if (annval instanceof TypeMirror) {
            return new SerTypeConst(getQualifiedNameUsingShell(((DeclaredType) annval).getDeclaration()));
        } else if (annval instanceof EnumConstantDeclaration) {
            EnumConstantDeclaration ecd = (EnumConstantDeclaration) annval;
            return new SerEnumConst(getQualifiedNameUsingShell(ecd.getDeclaringType()), ecd.getSimpleName());
        } else if (annval instanceof AnnotationMirror) {
            AnnotationMirror am = (AnnotationMirror) annval;
            return new SerAnnConst(getQualifiedNameUsingShell(am.getAnnotationType().getDeclaration()),
                    translate(am.getElementValues()));
        } else {
            return annval;
        }
    }

}
