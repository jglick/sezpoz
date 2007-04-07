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

/**
 * Represents one enum constant value.
 */
public final class SerEnumConst implements Serializable {

    private static final long serialVersionUID = 1L;

    /** fully qualified name of enumeration type */
    public final String enumName;
    /** name of enumeration "field" (constant name) */
    public final String constName;

    SerEnumConst(String enumName, String constName) {
        this.enumName = enumName;
        this.constName = constName;
    }

    public int hashCode() {
        return enumName.hashCode();
    }

    public boolean equals(Object obj) {
        if (!(obj instanceof SerEnumConst)) {
            return false;
        }
        SerEnumConst o = (SerEnumConst) obj;
        return enumName.equals(o.enumName) && constName.equals(o.constName);
    }

    public String toString() {
        return constName;
    }

}
