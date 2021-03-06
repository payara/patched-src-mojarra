/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
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

package com.sun.faces.facelets.tag.jsf.core;

import com.sun.faces.facelets.el.LegacyValueBinding;
import com.sun.faces.facelets.tag.TagHandlerImpl;

import com.sun.faces.facelets.tag.jsf.CompositeComponentTagHandler;
import javax.el.ELContext;
import javax.el.ExpressionFactory;
import javax.el.ValueExpression;
import javax.faces.component.ActionSource;
import javax.faces.component.ActionSource2;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ActionEvent;
import javax.faces.event.ActionListener;
import javax.faces.view.facelets.*;
import java.io.IOException;
import java.io.Serializable;
import javax.faces.view.ActionSource2AttachedObjectHandler;

public class SetPropertyActionListenerHandler extends TagHandlerImpl implements ActionSource2AttachedObjectHandler {

    private final TagAttribute value;

    private final TagAttribute target;

    public SetPropertyActionListenerHandler(TagConfig config) {
        super(config);
        this.value = this.getRequiredAttribute("value");
        this.target = this.getRequiredAttribute("target");
    }

    @Override
    public void apply(FaceletContext ctx, UIComponent parent)
            throws IOException {

        if (null == parent || !(ComponentHandler.isNew(parent))) {
            return;
        }
        if (parent instanceof ActionSource) {
            applyAttachedObject(ctx.getFacesContext(), parent);
        } else if (UIComponent.isCompositeComponent(parent)) {
            if (null == getFor()) {
                // PENDING(): I18N
                throw new TagException(this.tag,
                                       "actionListener tags nested within composite components must have a non-null \"for\" attribute");
            }
            // Allow the composite component to know about the target
            // component.
            CompositeComponentTagHandler.getAttachedObjectHandlers(parent)
                  .add(this);

        } else {
            throw new TagException(this.tag,
                                   "Parent is not of type ActionSource, type is: "
                                   + parent);
        }

    }

    @Override
    public void applyAttachedObject(FacesContext context, UIComponent parent) {
        FaceletContext ctx = (FaceletContext) context.getAttributes()
              .get(FaceletContext.FACELET_CONTEXT_KEY);

        ActionSource src = (ActionSource) parent;
        ValueExpression valueExpr = this.value.getValueExpression(ctx,
                    Object.class);
        ValueExpression targetExpr = this.target.getValueExpression(
                ctx, Object.class);

        ActionListener listener;

        if (src instanceof ActionSource2) {
            listener = new SetPropertyListener(valueExpr, targetExpr);
        } else {
            listener = new LegacySetPropertyListener(
                    new LegacyValueBinding(valueExpr),
                    new LegacyValueBinding(targetExpr));
        }

        src.addActionListener(listener);
    }


    @Override
    public String getFor() {
        String result = null;
        TagAttribute attr = this.getAttribute("for");

        if (null != attr) {
            if (attr.isLiteral()) {
                result = attr.getValue();
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                FaceletContext ctx = (FaceletContext) context.getAttributes().get(FaceletContext.FACELET_CONTEXT_KEY);
                result = (String)attr.getValueExpression(ctx, String.class).getValue(ctx);
            }
        }
        return result;
    }



    private static class LegacySetPropertyListener implements ActionListener,
            Serializable {

        private static final long serialVersionUID = 3004987947382293693L;

        private ValueBinding value;

        private ValueBinding target;

        public LegacySetPropertyListener() {
        };

        public LegacySetPropertyListener(ValueBinding value, ValueBinding target) {
            this.value = value;
            this.target = target;
        }

        @Override
        public void processAction(ActionEvent evt)
                throws AbortProcessingException {
            FacesContext faces = FacesContext.getCurrentInstance();
            Object valueObj = this.value.getValue(faces);
            this.target.setValue(faces, valueObj);
        }

    }

    private static class SetPropertyListener implements ActionListener,
            Serializable {

        private static final long serialVersionUID = -2760242070551459725L;

        private ValueExpression value;

        private ValueExpression target;

        public SetPropertyListener() {
        };

        public SetPropertyListener(ValueExpression value, ValueExpression target) {
            this.value = value;
            this.target = target;
        }

        @Override
        public void processAction(ActionEvent evt)
                throws AbortProcessingException {
            FacesContext faces = FacesContext.getCurrentInstance();
            ELContext el = faces.getELContext();
            Object valueObj = this.value.getValue(el);
             if (valueObj != null) {
                ExpressionFactory factory =
                      faces.getApplication().getExpressionFactory();
                valueObj = factory.coerceToType(valueObj, target.getType(el));
            }
            this.target.setValue(el, valueObj);
        }

    }

}
