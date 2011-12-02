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
