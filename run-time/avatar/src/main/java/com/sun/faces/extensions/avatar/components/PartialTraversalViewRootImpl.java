/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2018 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

/*
 * $Id: PartialTraversalViewRoot.java,v 1.4 2005/12/21 22:38:09 edburns Exp $
 */

package com.sun.faces.extensions.avatar.components;

import com.sun.faces.extensions.avatar.event.EventParser;
import com.sun.faces.extensions.avatar.lifecycle.AsyncResponse;
import com.sun.faces.extensions.common.util.Util;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.faces.FacesException;
import javax.faces.component.ActionSource;
import javax.faces.component.ContextCallback;
import javax.faces.component.EditableValueHolder;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRootCopy;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.context.ResponseWriterWrapper;
import javax.faces.convert.ConverterException;
import javax.faces.event.PhaseId;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * <p>Request Processing Lifecycle Operation</p>
 *
 * <p>On initial request, this instance behaves effectively like the
 * <code>UIViewRoot</code> from which it extends, delegating all
 * behavior to the superclass.</p>
 *
 * <p>On postback, the {@link #processDecodes}, {@link
 * #processValidators}, and {@link #processUpdates} methods have special
 * behaviour.  If {@link
 * com.sun.faces.extensions.avatar.lifecycle.AsyncResponse#isAjaxRequest}
 * returns <code>false</code>, the superclass implementation of the
 * respective method is called.  Otherwise, {@link
 * com.sun.faces.extensions.avatar.lifecycle.AsyncResponse#getExecuteSubtrees}
 * is called.  This returns a list of client ids suitable for the
 * <code>execute</code> portion of the request processing lifecycle.  If
 * the list is empty, call through to the superclass implementation of
 * the respective method.  Otherwise, for each client id in the list,
 * using <code>invokeOnComponent</code>, call the respective
 * <code>process*</code> method on the component with that clientId.
 * After all the client ids in the list have been handled for the
 * current phase, reflectively call <code>broadcastEvents()</code> on
 * the superclass passing the current <code>PhaseId</code>.</p>
 *
 * <p>On postback, the {@link #encodeBegin}, {@link #encodeChildren} and
 * {@link #encodeEnd} methods have special behavior.
 * If {@link
 * com.sun.faces.extensions.avatar.lifecycle.AsyncResponse#isAjaxRequest}
 * returns <code>false</code>, the superclass <code>encode*</code> methods are
 * called.  Otherwise, {@link
 * com.sun.faces.extensions.avatar.lifecycle.AsyncResponse#getRenderSubtrees}
 * is called.  This returns a list of client ids suitable for the {@link
 * javax.faces.lifecycle.Lifecycle#render} portion of the request
 * processing lifecycle.  If the list is empty, call through to the
 * superclass implementation of <code>encode*</code>.  Otherwise, for
 * each client id in the list, using {@link
 * javax.faces.component.UIComponent#invokeOnComponent}, call the
 * <code>encode*</code> methods on the component with that clientId.
 * If the list of subtrees to render for this request is non-empty, set
 * the response content-type and headers approriately for XML, and wrap
 * the rendered output from the components in the list as in the
 * following example.</p>
 *
<pre><code>
&lt;partial-response&gt;
  &lt;components&gt;
    &lt;render id="form:table"/&gt;
      &lt;markup&gt;&lt;![CDATA[
        Rendered content from component
      ]]&gt;&lt;/markup&gt;
      &lt;messages&gt;
        &lt;message&gt;The messages element is optional.  If present,
                 it is a list of FacesMessage.getSummary() output
        &lt;/message&gt;
      &lt;/messages&gt;
    &lt;/render&gt;
    &lt;!-- repeat for the appropriate number of components --&gt;
  &lt;/components&gt;
  &lt;state&gt;&lt;![CDATA[state information for this view ]]&gt;&lt;/state&gt;
&lt;/partial-response&gt;
</code></pre>
  *
  * if the <code>isRenderXML</code> value is <code>false</code>, assume
  * the renderers are handling the content-type, header, and component
  * encapsulation details and write nothing to the response.</p>
  *
  * <p>See {@link
  * com.sun.faces.extensions.avatar.lifecycle.PartialTraversalLifecycle} for
  * additional information about how this class helps
  * <code>PartialTraversalViewRoot</code> get its job done.</p>
 *
 * <p>This class extends <code>UIViewRootCopy</code>, which is a local copy
 * of <code>UIViewRoot</code> that makes the <code>broadcastEvents</code> method
 * public.  This is necessary to allow easily broadcasting events during the 
 * AJAX lifecycle.</p>
  *
  * @author edburns
  */
public class PartialTraversalViewRootImpl extends UIViewRootCopy implements Serializable, PartialTraversalViewRoot {
    
    public PartialTraversalViewRootImpl() {
        modifiedComponents = new ArrayList<UIComponent>();

        markImmediate = new Util.TreeTraversalCallback() {
            public boolean takeActionOnNode(FacesContext context, UIComponent comp) throws FacesException {
                if (comp instanceof ActionSource) {
                    ActionSource as = (ActionSource)comp;
                    if (!as.isImmediate()) {
                        as.setImmediate(true);
                        modifiedComponents.add(comp);
                    }
                } else if (comp instanceof EditableValueHolder) {
                    EditableValueHolder ev = (EditableValueHolder)comp;
                    if (!ev.isImmediate()) {
                        ev.setImmediate(true);
                        modifiedComponents.add(comp);
                    }
                }

                return true;
            }

        };
    }
    
    private transient List<UIComponent> modifiedComponents;
    
    private transient Util.TreeTraversalCallback markImmediate;
    
    public void postExecuteCleanup(FacesContext context) {
        for (UIComponent comp : modifiedComponents) {
            if (comp instanceof ActionSource) {
                ActionSource as = (ActionSource)comp;
                assert(as.isImmediate());
                as.setImmediate(false);
            } else if (comp instanceof EditableValueHolder) {
                EditableValueHolder ev = (EditableValueHolder)comp;
                assert(ev.isImmediate());
                ev.setImmediate(false);
            }
        }
        modifiedComponents.clear();
    }
    
    public void processDecodes(FacesContext context) {
        boolean invokedCallback = false;
        if (!AsyncResponse.isAjaxRequest()) {
            super.processDecodes(context);
            return;
        }
        AsyncResponse async = AsyncResponse.getInstance();
        modifiedComponents.clear();
        
        // If this is an immediate "render all" request and there are
        // no explicit execute subtrees and the user didn't request
        // execute: none...
        if (async.isImmediateAjaxRequest() && async.isRenderAll() &&
            async.getExecuteSubtrees().isEmpty() &&
            !async.isExecuteNone()) {
            // Traverse the entire view and mark every ActionSource or
            // EditableValueHolder as immediate.
            Util.prefixViewTraversal(context, this, markImmediate);
        }
        
        invokedCallback = invokeContextCallbackOnSubtrees(context,
                new PhaseAwareContextCallback(PhaseId.APPLY_REQUEST_VALUES));
        
        // Queue any events for this request in a context aware manner
        ResponseWriter writer = null;
        try {
            writer = async.getResponseWriter();
        } catch (IOException ex) {
            throw new FacesException(ex);
        }
        // Install the AjaxResponseWriter
        context.setResponseWriter(writer);
        
        EventParser.queueFacesEvents(context);
        
        if (!invokedCallback) {
            super.processDecodes(context);
        }
        this.broadcastEvents(context, PhaseId.APPLY_REQUEST_VALUES);
        
    }

    public void processValidators(FacesContext context) {
        if (!AsyncResponse.isAjaxRequest() ||
            !invokeContextCallbackOnSubtrees(context, 
                new PhaseAwareContextCallback(PhaseId.PROCESS_VALIDATIONS))) {
            super.processValidators(context);
            return;
        }
        this.broadcastEvents(context, PhaseId.PROCESS_VALIDATIONS);
    }

    public void processUpdates(FacesContext context) {
        if (!AsyncResponse.isAjaxRequest() ||
            !invokeContextCallbackOnSubtrees(context, 
                new PhaseAwareContextCallback(PhaseId.UPDATE_MODEL_VALUES))) {
            super.processUpdates(context);
            return;
        }
        this.broadcastEvents(context, PhaseId.UPDATE_MODEL_VALUES);
    }
    
    public boolean getRendersChildren() {
        AsyncResponse async = AsyncResponse.getInstance();
        boolean result = true;
        // If this is not an ajax request...
        if (!async.isAjaxRequest() || async.isRenderAll()) {
            // do default behavior
            result = super.getRendersChildren();
        }
        return result;
    }
    
    /**
     *	<p> Request scoped key to hold the original ResponseWriter between
     *	    encodeBegin and encodeEnd during Ajax requests.</p>
     */
    private static final String ORIGINAL_WRITER = AsyncResponse.FACES_PREFIX + 
            "origWriter";    

    public void encodeBegin(FacesContext context) throws IOException {
        AsyncResponse async = AsyncResponse.getInstance();
        // If this is not an ajax request...
        if (!async.isAjaxRequest()) {
            // do default behavior
            super.encodeBegin(context);
            return;
        }
        boolean renderAll = async.isRenderAll(),
                renderNone = async.isRenderNone();
        ResponseWriter orig = null, writer = null;
        
        try {
            // Turn on the response that has been embedded in the ViewHandler.
            async.setOnOffResponseEnabled(true);
            // If this is an ajax request, and it is a partial render request...

            if (!renderAll) {
                // replace the context's responseWriter with the AjaxResponseWriter
                // Get (and maybe create) the AjaxResponseWriter
                writer = async.getResponseWriter();
                orig = context.getResponseWriter();

		// Save the new writer for encodeEnd
		context.getExternalContext().getRequestMap().
                    put(ORIGINAL_WRITER, orig);

                // Install the AjaxResponseWriter
                context.setResponseWriter(writer);
            }
            
            this.encodePartialResponseBegin(context);

            if (renderAll) {
                writer = context.getResponseWriter();
                // If this is a "render all via ajax" request,
                // make sure to wrap the entire page in a <render> elemnt
                // with the special id of VIEW_ROOT_ID.  This is how the client
                // JavaScript knows how to replace the entire document with 
                // this response.
                writer.startElement("render", this);
                writer.writeAttribute("id", AsyncResponse.VIEW_ROOT_ID, "id");
                writer.startElement("markup", this);
                writer.write("<![CDATA[");
                
                // setup up a writer which will escape any CDATA sections
                context.setResponseWriter(new EscapeCDATAWriter(writer));
            }
        } catch (IOException ex) {
	    cleanupAfterView(context);
        } catch (RuntimeException ex) {
	    cleanupAfterView(context);
	    
	    // Throw the exception
	    throw ex;
        }
    }       

    public void encodeChildren(FacesContext context) throws IOException {
        AsyncResponse async = AsyncResponse.getInstance(false);
        // If this is not an ajax request...
        if ((async == null) || !async.isAjaxRequest() || async.isRenderAll()) {
            // Full request or ajax encode All request, operate normally
            super.encodeChildren(context);
        }
	else {
            // If the context callback was not invoked on any subtrees
            // and the client did not explicitly request that no
            // subtrees be rendered...
            if (!invokeContextCallbackOnSubtrees(context, 
                    new PhaseAwareContextCallback(PhaseId.RENDER_RESPONSE)) &&
		!async.isRenderNone()) {
                assert(false);
            }
	}
    }
    

    public void encodeEnd(FacesContext context) throws IOException {
        AsyncResponse async = AsyncResponse.getInstance(false);
        // If this is not an ajax request...
        if ((async == null) || !async.isAjaxRequest()) {
            // do default behavior
            super.encodeEnd(context);
            return;
        }


        try {
            if (async.isRenderAll()) {
		EscapeCDATAWriter cdataWriter = (EscapeCDATAWriter)
		    context.getResponseWriter();
		ResponseWriter writer = cdataWriter.getWrapped();

                // revert the writer and finish up
                context.setResponseWriter(writer);
                writer.write("]]>");
                writer.endElement("markup");
                writer.endElement("render");
                // then bail out.  
                return;
            }
            
	    
            this.encodePartialResponseEnd(context);
            
        } catch (IOException ioe) {
            
        } finally {
            cleanupAfterView(context);
        }
    }


    
    public void encodePartialResponseBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        AsyncResponse async = AsyncResponse.getInstance();
        
        ExternalContext extContext = context.getExternalContext();
        // If the client did not explicitly request that no subtrees be rendered...
        if (!async.isRenderNone()) {
            // prepare to render the partial response.
            if (extContext.getResponse() instanceof HttpServletResponse) {
                HttpServletResponse servletResponse = (HttpServletResponse)
                extContext.getResponse();
                servletResponse.setContentType("text/xml");
                servletResponse.setHeader("Cache-Control", "no-cache");
                String xjson =
                        extContext.getRequestHeaderMap().get(AsyncResponse.XJSON_HEADER);
                if (null != xjson) {
                    servletResponse.setHeader(AsyncResponse.XJSON_HEADER,
                            xjson);
                }
                
                writer.startElement("partial-response", this);
                writer.startElement("components", this);
            }
        }
    }
    
    public void encodePartialResponseEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        AsyncResponse async = AsyncResponse.getInstance();
        
        // PENDING(edburns): The core JSF spec does not dispatch events for
        // Render Response.  We need to do it here so that events that require
        // template text can rely on the tree including template text.
        this.broadcastEvents(context, PhaseId.RENDER_RESPONSE);
        
        // If the client did not explicitly request that no subtrees be rendered...
        if (!async.isRenderNone()) {
            writer.endElement("components");
        }
        
    }
    
    
    protected void broadcastEvents(FacesContext context, PhaseId phaseId) {
        String postViewId, preViewId = context.getViewRoot().getViewId();
        // Broadcast the regular FacesEvents
        super.broadcastEvents(context, phaseId);
        // Do our extra special MethodExpression invocation
        EventParser.invokeComponentMethodCallbackForPhase(context, phaseId);
        postViewId = context.getViewRoot().getViewId();
        
        // If the view id changed as result of the broadcastEvents...
        if (!postViewId.equals(preViewId)) {
            // Advise the browser to re-render the entire view.
            AsyncResponse.getInstance().setRenderAll(true);
        }
        
    }   
    
    
    /**
     *
     */
    private void cleanupAfterView(FacesContext context) {
	ResponseWriter orig = (ResponseWriter) context.getExternalContext().
	    getRequestMap().get(ORIGINAL_WRITER);
	assert(null != orig);
	
        // PENDING(edburns): this is a big hack to get around the
        // way the JSP based faces implementation handles the
        // after view content.
        // We will have to do something different for other implementations.
        // This is not a problem for Facelets.
        context.getExternalContext().getRequestMap().remove("com.sun.faces.AFTER_VIEW_CONTENT");

        // move aside the AjaxResponseWriter
        if (null != orig) {
            context.setResponseWriter(orig);
        }
    }

    
    private boolean invokeContextCallbackOnSubtrees(FacesContext context, 
            PhaseAwareContextCallback cb) {
        AsyncResponse async = AsyncResponse.getInstance();
        List<String> subtrees = null;
        
        // If this callback is intended for RENDER_RESPONSE, use
        // async.getRenderSubtrees().  Otherwise, use async.getExecuteSubtrees().
        // If async.getExecuteSubtrees() is empty, use async.getRenderSubtrees().
        
        if (cb.getPhaseId() == PhaseId.RENDER_RESPONSE) {
            subtrees = async.getRenderSubtrees();
        }
        else {
            subtrees = async.getExecuteSubtrees();
            if (subtrees.isEmpty()) {
                subtrees = async.getRenderSubtrees();
            }
        }
        
        boolean result = false;
        
        for (String cur : subtrees) {
            if (this.invokeOnComponent(context, cur, cb)) {
                result = true;
            }
        }
        return result;
    }
    
    private class PhaseAwareContextCallback implements ContextCallback {

        private PhaseId curPhase = null;
        private PhaseAwareContextCallback(PhaseId curPhase) {
            this.curPhase = curPhase;
        }
        
        private PhaseId getPhaseId() {
            return curPhase;
        }
        
        public void invokeContextCallback(FacesContext facesContext, UIComponent comp) {
            try {
                ConverterException converterException = null;
                
                if (curPhase == PhaseId.APPLY_REQUEST_VALUES) {
                    // If the user requested an immediate request
                    // Make sure to set the immediate flag.
                    if (AsyncResponse.isImmediateAjaxRequest()) {
                        PartialTraversalViewRootImpl.this.markImmediate.takeActionOnNode(facesContext, comp);
                    }

                    comp.processDecodes(facesContext);
                } else if (curPhase == PhaseId.PROCESS_VALIDATIONS) {
                    comp.processValidators(facesContext);
                } else if (curPhase == PhaseId.UPDATE_MODEL_VALUES) {
                    comp.processUpdates(facesContext);
                } else if (curPhase == PhaseId.RENDER_RESPONSE) {
                    
                    if (comp.isRendered()) {
                        ResponseWriter writer = AsyncResponse.getInstance().getResponseWriter();

                        writer.startElement("render", comp);
                        writer.writeAttribute("id", comp.getClientId(facesContext), "id");
                        try {
                            writer.startElement("markup", comp);
                            writer.write("<![CDATA[");

                            // setup up a writer which will escape any CDATA sections
                            facesContext.setResponseWriter(new EscapeCDATAWriter(writer));

                            // do the default behavior...
                            comp.encodeAll(facesContext);

                            // revert the write and finish up
                            facesContext.setResponseWriter(writer);
                            writer.write("]]>");
                            writer.endElement("markup");
                        }
                        catch (ConverterException ce) {
                            converterException = ce;
                        }
                        writer.endElement("render");
                    }
                }
                else {
                    throw new IllegalStateException("I18N: Unexpected PhaseId passed to PhaseAwareContextCallback: " + curPhase.toString());
                }
                    
            }
            catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        
    }
    
    private class EscapeCDATAWriter extends ResponseWriterWrapper {

        private ResponseWriter toWrap = null;
        public EscapeCDATAWriter(ResponseWriter toWrap) {
            this.toWrap = toWrap;
        }
        protected ResponseWriter getWrapped() { 
            return toWrap; 
        }

        public void write(String string) throws IOException {
            super.write(string.replace("]]>", "]]]]><![CDATA[>"));
        }
    }
    
}
