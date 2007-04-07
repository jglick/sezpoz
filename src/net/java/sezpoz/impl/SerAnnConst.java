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
 * Representation of a nested annotation constant.
 */
public final class SerAnnConst implements Serializable {

    private static final long serialVersionUID = 1L;

    /** fully qualified name of annotation type */
    public final String name;
    /** values of annotation attrs, as in {@link #SerAnnotatedElement} */
    public final TreeMap<String,Object> values;

    SerAnnConst(String name, TreeMap<String,Object> values) {
        this.name = name;
        this.values = values;
    }

    public int hashCode() {
        return name.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SerAnnConst)) {
            return false;
        }
        SerAnnConst o = (SerAnnConst) obj;
        return name.equals(o.name) && values.equals(o.values);
    }

    public String toString() {
        return "@" + name + values;
    }
    
}
