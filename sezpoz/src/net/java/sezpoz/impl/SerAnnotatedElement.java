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

import java.io.Serializable;
import java.util.TreeMap;

/**
 * Represents one annotated element (class etc.) with a particular list of values.
 * One META-INF/annotations/* file is a sequence of serialized SerAnnotatedElement
 * instances, terminated by a null.
 */
public final class SerAnnotatedElement implements Serializable {

    private static final long serialVersionUID = 1L;

    /** fully qualified name of class */
    public final String className;
    /** mame of method or field within class, or null for whole class */
    public final String memberName;
    /** true for method, false for class or field */
    public final boolean isMethod;
    /** values of annotation, as primitive wrappers, String's, ArrayList's (for arrays), or Ser*Const objects */
    public final TreeMap<String,Object> values;

    SerAnnotatedElement(String className, String memberName, boolean isMethod, TreeMap<String,Object> values) {
        this.className = className;
        this.memberName = memberName;
        this.isMethod = isMethod;
        this.values = values;
    }

    public int hashCode() {
        return className.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SerAnnotatedElement)) {
            return false;
        }
        SerAnnotatedElement o = (SerAnnotatedElement) obj;
        return className.equals(o.className) &&
                ((memberName == null) ? (o.memberName == null) : memberName.equals(o.memberName)) &&
                isMethod == o.isMethod &&
                values.equals(o.values);
    }

    public String toString() {
        StringBuffer b = new StringBuffer(className);
        if (memberName != null) {
            b.append('#');
            b.append(memberName);
            if (isMethod) {
                b.append("()");
            }
        }
        b.append(values);
        return b.toString();
    }

}
