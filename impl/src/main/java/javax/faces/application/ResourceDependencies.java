/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package javax.faces.application;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * <p class="changed_added_2_0">
 * Container annotation to specify multiple {@link ResourceDependency} annotations on a single
 * class. Example:
 * </p>
 * 
 * <pre>
 * <code>
&#0064;ResourceDependencies( {
  &#0064;ResourceDependency(library="corporate", name="css_master.css"),
  &#0064;ResourceDependency(library="client01", name="layout.css"),
  &#0064;ResourceDependency(library="corporate", name="typography.css"),
  &#0064;ResourceDependency(library="client01", name="colorAndMedia.css"),
  &#0064;ResourceDependency(library="corporate", name="table2.css"),
  &#0064;ResourceDependency(library="fancy", name="commontaskssection.css"),
  &#0064;ResourceDependency(library="fancy", name="progressBar.css"),
  &#0064;ResourceDependency(library="fancy", name="css_ns6up.css")
                       })
</code>
 * </pre>
 * 
 * 
 * <div class="changed_added_2_0">
 * 
 * <p>
 * The action described in {@link ResourceDependency} must be taken for each
 * <code>&#0064;ResourceDependency</code> present in the container annotation.
 * </p>
 * 
 * </div>
 */
@Retention(RUNTIME)
@Target(TYPE)
@Inherited
public @interface ResourceDependencies {

    ResourceDependency[] value();
}