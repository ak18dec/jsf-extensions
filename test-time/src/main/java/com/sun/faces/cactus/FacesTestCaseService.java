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
 * $Id: FacesTestCaseService.java,v 1.2 2005/10/26 02:24:05 edburns Exp $
 */

// FacesTestCaseService.java

package com.sun.faces.cactus;

import com.sun.faces.RIConstants;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;

import javax.faces.FactoryFinder;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.context.ResponseWriter;
import javax.faces.lifecycle.Lifecycle;
import javax.faces.lifecycle.LifecycleFactory;
import javax.faces.webapp.FacesServlet;
import javax.servlet.ServletContextEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;

import com.sun.faces.application.ApplicationAssociate;
import com.sun.faces.config.ConfigureListener;
import com.sun.faces.util.Util;

import org.apache.cactus.server.ServletContextWrapper;


/**
 * Subclasses of ServletTestCase and JspTestCase use an instance of this
 * class to handle behavior specific to Faces TestCases.  You may
 * recognize this as using object compositition vs multiple inheritance.
 * <P>
 * <p/>
 * <p/>
 * <B>Lifetime And Scope</B> <P> Same as the JspTestCase or
 * ServletTestCase instance that uses it.
 *
 * @version $Id: FacesTestCaseService.java,v 1.2 2005/10/26 02:24:05 edburns Exp $
 * @see	com.sun.faces.context.FacesContextFactoryImpl
 * @see	com.sun.faces.context.FacesContextImpl
 */

public class FacesTestCaseService extends Object {

//
// Protected Constants
//

    /**
     * Things used as names and values in the System.Properties table.
     */

    public static final String ENTER_CALLED = "enterCalled";
    public static final String EXIT_CALLED = "exitCalled";
    public static final String EMPTY = "empty";



//
// Class Variables
//

//
// Instance Variables
//

// Attribute Instance Variables

// Relationship Instance Variables

    protected FacesTestCase facesTestCase = null;

    protected FacesContextFactory facesContextFactory = null;

    protected FacesContext facesContext = null;

    protected Lifecycle lifecycle = null;

//
// Constructors and Initializers    
//

    public FacesTestCaseService(FacesTestCase newFacesTestCase) {
        facesTestCase = newFacesTestCase;
    }


//
// Class methods
//

//
// General Methods
//

    public FacesContext getFacesContext() {
        return facesContext;
    }


    public FacesContextFactory getFacesContextFactory() {
        return facesContextFactory;
    }


    public void setUp() {
        HttpServletResponse response = null;
        TestingUtil.setUnitTestModeEnabled(true);

	// make sure the ApplicationAssociate is aware of the ServletContext
	StoreServletContext storeSC = 
	    new StoreServletContext();
	storeSC.setServletContext(facesTestCase.getConfig().getServletContext());

        // make sure the default factories are found, even if they have been
        // cleared before.
	FactoryFinder.releaseFactories();
        FactoryFinder.setFactory(FactoryFinder.APPLICATION_FACTORY,
                                 "com.sun.faces.application.ApplicationFactoryImpl");
        FactoryFinder.setFactory(FactoryFinder.FACES_CONTEXT_FACTORY,
                                 "com.sun.faces.context.FacesContextFactoryImpl");
        FactoryFinder.setFactory(FactoryFinder.LIFECYCLE_FACTORY,
                                 "com.sun.faces.lifecycle.LifecycleFactoryImpl");
        FactoryFinder.setFactory(FactoryFinder.RENDER_KIT_FACTORY,
                                 "com.sun.faces.renderkit.RenderKitFactoryImpl");

	 ApplicationAssociate.clearInstance(storeSC.getServletContextWrapper());
        Util.verifyFactoriesAndInitDefaultRenderKit(
            facesTestCase.getConfig().getServletContext());

        facesContextFactory = (FacesContextFactory)
            FactoryFinder.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
        assert (null != facesContextFactory);

        // Since we run using tomcat's deploy targets, we must obtain the
        // absolute path to where we are to write our output files.
        String testRootDir =
            facesTestCase.getConfig().getServletContext().getInitParameter(
                "testRootDir");

        assert (null != testRootDir);
        facesTestCase.setTestRootDir(testRootDir);
       
        // See if the testcase wants to have its output sent to a file.
        if (facesTestCase.sendResponseToFile()) {
            response =
                new FileOutputResponseWrapper(facesTestCase.getResponse(),
                    testRootDir );
        } else {
            response = facesTestCase.getResponse();
        }

        LifecycleFactory factory = (LifecycleFactory)
            FactoryFinder.getFactory(FactoryFinder.LIFECYCLE_FACTORY);
        assert (null != factory);
        lifecycle = factory.getLifecycle(LifecycleFactory.DEFAULT_LIFECYCLE);
        assert (null != lifecycle);


        facesContext =
            facesContextFactory.getFacesContext(facesTestCase.getConfig().
                                                getServletContext(),
                                                facesTestCase.getRequest(),
                                                response, lifecycle);
        assert (null != facesContext);
        if (facesTestCase.sendWriterToFile()) {
            ResponseWriter responseWriter = 
                    new FileOutputResponseWriter(testRootDir);
            facesContext.setResponseWriter(responseWriter);
        }
        
        TestBean testBean = new TestBean();
        facesContext.getExternalContext().getSessionMap().put("TestBean",
                                                              testBean);

        Iterator paramNames = getFacesContext().getExternalContext()
            .getRequestParameterNames();
        while (paramNames.hasNext()) {
            String curName = (String) paramNames.next();

            System.out.println(curName + "=" +
                               getFacesContext().getExternalContext().
                               getRequestParameterMap().get(curName));
        }

        ConfigureListener configListener = new ConfigureListener();
        ServletContextEvent e =
            new ServletContextEvent(
                facesTestCase.getConfig().getServletContext());

        // make sure this gets called once per ServletContext instance.
        if (null ==
            (facesTestCase.getConfig().getServletContext().
            getAttribute(FacesServlet.CONFIG_FILES_ATTR))) {

            configListener.contextInitialized(e);
        }

	storeSC.setServletContext(facesTestCase.getConfig().getServletContext());
        
        // remove any view state in request state
        facesContext.getExternalContext().getRequestMap().remove(RIConstants.LOGICAL_VIEW_MAP);

    }


    public void tearDown() {
        // make sure this gets called!
        if ( facesTestCase.getConfig().getServletContext() != null ) {
            facesTestCase.getConfig().getServletContext()
                .removeAttribute(RIConstants.HTML_BASIC_RENDER_KIT);
        }
        
        ConfigureListener configListener = new ConfigureListener();
        ServletContextEvent e =
            new ServletContextEvent(
                facesTestCase.getConfig().getServletContext());
        configListener.contextDestroyed(e);

        // make sure session is not null. It will null in case release
        // was invoked.
        try {
            if (facesContext.getExternalContext() != null) {
                if (facesContext.getExternalContext().getSession(true) != null) {
                    facesContext.getExternalContext().getSessionMap().remove(
                        "TestBean");
                }
            }
        } catch (IllegalStateException ie) {
        }
    }

    
    public boolean verifyExpectedOutput() {
        boolean result = false;
        CompareFiles cf = new CompareFiles();
        String errorMessage = null;
        String outputFileName = null;
        String correctFileName = null;

        // If this testcase doesn't participate in file comparison
        if (!facesTestCase.sendResponseToFile() &&
            (!facesTestCase.sendWriterToFile()) &&
            (null == facesTestCase.getExpectedOutputFilename())) {
            return true;
        }

        if (facesTestCase.sendResponseToFile()) {
            outputFileName = FileOutputResponseWrapper.FACES_RESPONSE_FILENAME;
        } else {
            outputFileName = FileOutputResponseWriter.RESPONSE_WRITER_FILENAME;
        }
        correctFileName = FileOutputResponseWriter.FACES_RESPONSE_ROOT +
            facesTestCase.getExpectedOutputFilename();

        errorMessage = "File Comparison failed: diff -u " + outputFileName +
            " " +
            correctFileName;

        ArrayList ignoreList = null;
        String[] ignore = null;

        if (null != (ignore = facesTestCase.getLinesToIgnore())) {
            ignoreList = new ArrayList();
            for (int i = 0; i < ignore.length; i++) {
                ignoreList.add(ignore[i]);
            }
        }

        try {
            result =
                cf.filesIdentical(outputFileName, correctFileName, ignoreList);
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }

        if (!result) {
            System.out.println(errorMessage);
        }
        System.out.println("VERIFY:" + result);
        return result;
    }


    /**
     * This utiity method searches the specified file (line by line) for the occurence
     * of the specified string.
     */
    public boolean verifyExpectedStringInOutput(String str) {
        boolean exists = false;
        String outputFileName = null;

        // If this testcase doesn't deal with output file(s)
        if (!facesTestCase.sendResponseToFile() &&
            (!facesTestCase.sendWriterToFile()) &&
            (null == facesTestCase.getExpectedOutputFilename())) {
            return true;
        }

        if (facesTestCase.sendResponseToFile()) {
            outputFileName = FileOutputResponseWrapper.FACES_RESPONSE_FILENAME;
        } else {
            outputFileName = FileOutputResponseWriter.RESPONSE_WRITER_FILENAME;
        }

        try {
            File fileToCheck = new File(outputFileName);
            FileReader fileReader = new FileReader(fileToCheck);
            LineNumberReader lineReader = new LineNumberReader(fileReader);
            String line = lineReader.readLine().trim();

            while (null != line) {
                if (line.indexOf(str) >= 0) {
                    exists = true;
                    break;
                }
                line = lineReader.readLine();
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
            e.printStackTrace();
        }
        return exists;
    }


    public boolean isMember(String toTest, String[] set) {
        int
            len = set.length,
            i = 0;
        for (i = 0; i < len; i++) {
            if (set[i].equals(toTest)) {
                return true;
            }
        }
        return false;
    }


    /**
     * @return true iff every element in subset is present in the iterator.
     */

    public boolean isSubset(String[] subset, Iterator superset) {
        int i, len = subset.length;
        boolean[] hits = new boolean[len];
        String cur = null;
        for (i = 0; i < len; i++) {
            hits[i] = false;
        }


        // for each element in the superset, go through the entire subset,
        // marking our "hits" array if there is a match.
        while (superset.hasNext()) {
            cur = (String) superset.next();
            for (i = 0; i < len; i++) {
                if (cur.equals(subset[i])) {
                    hits[i] = true;
                }
            }
        }

        // if any of the hits array is false, return false;
        for (i = 0; i < len; i++) {
            if (!hits[i]) {
                return false;
            }
        }
        return true;
    }


    /**
     * <p>This method allows comparing the attribute sets of two
     * HttpServletRequests for equality.</p>
     *
     * @return true if every attribute in in request1 has an analog in
     *         request2, with the same value as in request1, and the converse is true
     *         as well.
     */


    public boolean requestsHaveSameAttributeSet(HttpServletRequest request1,
                                                HttpServletRequest request2) {
        Enumeration attrNames = request1.getAttributeNames();
        String curAttr;
        Object valA, valB;

        // make sure every name/value pair in request1 is the same
        // name/value pare in request2.

        while (attrNames.hasMoreElements()) {
            curAttr = (String) attrNames.nextElement();
            valA = request1.getAttribute(curAttr);
            valB = request2.getAttribute(curAttr);
            if (null != valA && null != valB) {
                // if any of the values are not equal, return false
                if (!valA.equals(valB)) {
                    return false;
                }
            } else if (null != valA || null != valB) {
                // one of the values is null, therefore, not equal, return false
                return false;
            }
        }

        // make sure every name/value pair in request2 is the same
        // name/value pare in request1.
        attrNames = request2.getAttributeNames();

        while (attrNames.hasMoreElements()) {
            curAttr = (String) attrNames.nextElement();
            valA = request2.getAttribute(curAttr);
            valB = request1.getAttribute(curAttr);
            if (null != valA && null != valB) {
                // if any of the values are not equal, return false
                if (!valA.equals(valB)) {
                    return false;
                }
            } else if (null != valA || null != valB) {
                // one of the values is null, therefore, not equal, return false
                return false;
            }
        }
        return true;
    }


    public void loadFromInitParam(String paramValue) {
        final String paramVal = paramValue;

        // clear out the attr that was set in the servletcontext attr set.
        facesTestCase.getConfig().getServletContext().removeAttribute(
            FacesServlet.CONFIG_FILES_ATTR);
	ApplicationAssociate.clearInstance(facesContext.getExternalContext());
        // clear out the renderKit factory
        FactoryFinder.releaseFactories();

        // work around a bug in cactus where calling
        // config.setInitParameter() doesn't cause
        // servletContext.getInitParameter() to relfect the call.

        ServletContextWrapper sc =
            new ServletContextWrapper(
                facesTestCase.getConfig().getServletContext()) {
                public String getInitParameter(String theName) {
                    if (null != theName &&
                        theName.equals(FacesServlet.CONFIG_FILES_ATTR)) {
                        return paramVal;
                    }
                    return super.getInitParameter(theName);
                }
            };

        ConfigureListener configListener = new ConfigureListener();
        ServletContextEvent e =
            new ServletContextEvent(sc);
        configListener.contextDestroyed(e);
        configListener.contextInitialized(e);
    }

    public Object wrapRequestToHideParameters() {
	Object oldRequest = 
	    getFacesContext().getExternalContext().getRequest();
	
	HttpServletRequest wrapper = 
	    new HttpServletRequestWrapper((HttpServletRequest)oldRequest) {
		public java.util.Enumeration getParameterNames() {
		    return new java.util.Enumeration() {
			    public boolean hasMoreElements() {
				return( false );
			    }
			    
			    public Object nextElement() {
				return new java.util.NoSuchElementException();
			    }
			};
		}
	    };
	getFacesContext().getExternalContext().setRequest(wrapper);
	return oldRequest;
    }

    public void unwrapRequestToShowParameters(Object oldRequest) {
	getFacesContext().getExternalContext().setRequest(oldRequest);
    }


} // end of class FacesTestCaseService
