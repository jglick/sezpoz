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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker for an annotation type which can be indexed.
 * You can then use {@link Index} to find all annotated elements
 * together with the annotations without necessarily loading any
 * classes.
 * <p>
 * The annotation may use any {@link java.lang.annotation.RetentionPolicy}
 * and must permit targets of type {@link java.lang.annotation.ElementType#TYPE},
 * {@link java.lang.annotation.ElementType#METHOD}, or {@link java.lang.annotation.ElementType#FIELD}
 * (but no others). It should not be {@link java.lang.annotation.Inherited}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.ANNOTATION_TYPE})
public @interface Indexable {
    
    /**
     * Optional indication of a type to which the resolved instance
     * must be assignable.
     * @return the type of instance
     */
    Class<?> type() default Object.class;
    
}
