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
    Class type() default Object.class;
    
}
