/*
 * $Id: ProcessingContextViewRoot.java,v 1.4 2005/12/21 22:38:09 edburns Exp $
 */

/*
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * https://javaserverfaces.dev.java.net/CDDL.html or
 * legal/CDDLv1.0.txt.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at legal/CDDLv1.0.txt.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * [Name of File] [ver.__] [Date]
 *
 * Copyright 2005 Sun Microsystems Inc. All Rights Reserved
 */

package com.sun.faces.components;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.faces.component.ContextCallback;
import javax.faces.component.NamingContainer;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.servlet.http.HttpServletResponse;

/**
 *
 * @author edburns
 */
public class ProcessingContextViewRoot extends UIViewRoot {
    
    /** Creates a new instance of ProcessingContextViewRoot */
    public ProcessingContextViewRoot() {
    }
    
    public String getRendererType() {
        return (COMPONENT_FAMILY);
        
    }
    
    private transient DelegateRootIterator delegateRootIter = null;
    private transient boolean invokeOnComponent = false;

    /**
     * Holds value of property processingContexts.
     */
    private transient List<ProcessingContext> processingContexts;

    public List<ProcessingContext> getProcessingContexts() {
        String param = null;
        String [] pcs = null;
        Map requestMap = FacesContext.getCurrentInstance().
                getExternalContext().getRequestParameterMap();
        
        if (null != processingContexts) {
            return processingContexts;
        }

        // If there is no processingContext request parameter
        if (!requestMap.containsKey(PROCESSING_CONTEXTS_REQUEST_PARAM_NAME)) {
            return null;
        }
        
        processingContexts = new ArrayList<ProcessingContext>();
        
        // If we have a processingContext Request Parameter
        param = 
            requestMap.get(PROCESSING_CONTEXTS_REQUEST_PARAM_NAME).toString();
        if (null != (pcs = param.split(",[ \t]*"))) {
            for (String cur : pcs) {
                cur = cur.trim();
                // Only absolute clientIds are valid as ProcessingContext identifiers
                if (cur.startsWith("" + NamingContainer.SEPARATOR_CHAR)) {
                    processingContexts.add(new ProcessingContext(cur.trim()));
                }
            }
        }
        // Catch a mal-formed param value here
        if (0 == processingContexts.size()) {
            processingContexts = null;
        }
        
        return this.processingContexts;
    }

    public void setProcessingContexts(List<ProcessingContext> processingContexts) {

        this.processingContexts = processingContexts;
    }

    public static final String PROCESSING_CONTEXTS_REQUEST_PARAM_NAME = "com.sun.faces.PCtxt";
    
    private List<UIComponent> getDelegateRoots() {
        // Otherwise, we need to generate the list of delegateRoots.
        final UIComponent [] cur = new UIComponent[1];
        cur[0] = null;
        String pcClientId = null;

        List<UIComponent> delegateRoots = new ArrayList<UIComponent>();
        List<ProcessingContext> pcs = getProcessingContexts();
        if (null == pcs) {
            return null;
        }
        Iterator<ProcessingContext> pcIter = pcs.iterator();
        // Build up a list of UIComponents, one for each processing context.
        while (pcIter.hasNext()) {
            pcClientId = pcIter.next().getClientId();
            assert(pcClientId.startsWith("" + NamingContainer.SEPARATOR_CHAR));
            pcClientId = pcClientId.substring(1);
            // PENDING(edburns): pain point.  An incorrectly specified pcId
            // can cause this method to break.  Need to catch this condition
            // and deal with it gracefully.
            ContextCallback cb = new ContextCallback() {
                public void invokeContextCallback(FacesContext facesContext, UIComponent comp) {
                    cur[0] = comp;
                }
                
            };
            this.invokeOnComponent = true;
            try {
                this.invokeOnComponent(FacesContext.getCurrentInstance(), 
                        pcClientId, cb);
            }
            finally {
                invokeOnComponent = false;
            }
            if (null != cur[0]) {
                delegateRoots.add(cur[0]);
            }
        }
        return delegateRoots;
    }
    
    public Iterator<UIComponent> getFacetsAndChildren() {
        // If we have no processing contexts, we can have no delegateRoots.
        if (null == getProcessingContexts() || invokeOnComponent) {
            // Therefore, just return the real ViewRoot's facetsAndChildren.
            return super.getFacetsAndChildren();
        }

        if (null == delegateRootIter) {
            delegateRootIter = new DelegateRootIterator();
            List<UIComponent> roots = getDelegateRoots();
            delegateRootIter.setRoots(roots);
        }
        else {
            delegateRootIter.reset();
        }
        assert(null != delegateRootIter);
        
        return delegateRootIter;
    }
    
    public void encodeAll(FacesContext context) throws IOException {
        List<UIComponent> roots = getDelegateRoots();
        if (null == roots) {
            super.encodeAll(context);
        }
        else {
            ExternalContext extContext = context.getExternalContext();
            if (extContext.getResponse() instanceof HttpServletResponse) {
                HttpServletResponse servletResponse = (HttpServletResponse)
                    extContext.getResponse();
                servletResponse.setContentType("text/xml");
                servletResponse.setHeader("Cache-Control", "no-cache");
            }
            ResponseWriter responseWriter = context.getResponseWriter();
            responseWriter.startElement("processing-context-responses", this);
            Map<String, Object> requestMap = 
                    context.getExternalContext().getRequestMap();
            requestMap.put(PROCESSING_CONTEXTS_REQUEST_PARAM_NAME, 
                    PROCESSING_CONTEXTS_REQUEST_PARAM_NAME);
            try {
                for (UIComponent curRoot : roots) {
                    curRoot.encodeAll(context);
                }
            }
            finally {
                requestMap.remove(PROCESSING_CONTEXTS_REQUEST_PARAM_NAME);
            }
            responseWriter.endElement("processing-context-responses");
        }
    }
    

    
 class DelegateRootIterator extends Object implements Iterator<UIComponent> {

     private Iterator<UIComponent> curIter = null;
     
     private List<UIComponent> roots = null;
     private Iterator<UIComponent> rootsIter = null;
     
     /**
      * For each component in the list of UIComponents, call its
      * getFacetsAndChildren() method.
      */
     
     public void setRoots(List<UIComponent> roots) {
         this.roots = roots;
         this.rootsIter = roots.iterator();
     }
     
     public void reset() {
         curIter = null;
         assert(null != roots);
         rootsIter = roots.iterator();
     }
     
     public void remove() {
         throw new UnsupportedOperationException();
     }
     public UIComponent next() {
         UIComponent result = null;
         if (hasNext()) {
             assert(null != curIter);
             result = curIter.next();
         }
         return result;
     }
     public boolean hasNext() {
         boolean result = false;
         // If we need to proceed to the next root, obtain
         // it and get its iterator.
         if (null == curIter) {
             // If we have more roots:
             if (rootsIter.hasNext()) {
                 // Get the next iterator.
                 curIter = rootsIter.next().getFacetsAndChildren();
             }
         }
         // If we successfully obtained the current iterator...
         if (null != curIter) {
             // but there are no more elements in it...
             if (!(result = curIter.hasNext())) {
                 curIter = null;
                 // recurse on the next iterator
                 result = hasNext();
             }
         }
         return result;
         
     }
  }
    
}