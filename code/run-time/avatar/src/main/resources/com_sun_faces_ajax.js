/*
 * $Id: devtime.js,v 1.5 2006/01/13 16:05:28 edburns Exp $
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

var g_zones = [];

dojo.require("dojo.io.*");
dojo.require("dojo.event.*");
dojo.require("dojo.string.*");

function ajaxifyChildren(target, eventType, eventHook) {
    if (null == target.isAjaxified && 
        target.hasChildNodes()) {
	for (var i = 0; i < target.childNodes.length; i++) {
	    takeActionAndTraverseTree(target, target.childNodes[i], 
				      moveAsideEventType, eventType, 
				      eventHook);
	}
    }
    target.isAjaxified = true;
    return false;
}

function moveAsideEventType(ajaxZone, element, eventType, eventHook) {
    handler = new Object();
    handler["eventHook"] = eventHook;
    handler["eventType"] = eventType;
    handler["element"] = element;
    handler["originalScript"] = element[eventType];
    handler["aroundHandler"] = function (invocation) {
	// invocation.args[0] is the event.
	var props = new Object();
	var pctxts = "";
	var zones = dj_global.g_zones;
        var originalScript = this["originalScript"];
        if (null != originalScript) {
          originalScript = originalScript.toString();
        }

	// Here is where we invoke the user script to populate the "props"
	// as appropriate to this particular zone.
	dj_global[this["eventHook"]](ajaxZone, this["element"], 
				     originalScript, props, invocation);

	for (var i = 0; i < zones.length; i++) {
	    pctxts = pctxts + zones[i];
	    if (!(zones.length - 1 == i)) {
		pctxts = pctxts + ",";
	    }
	}
	if (0 < pctxts.length) {
	    props['com.sun.faces.PCtxt'] = pctxts;
	}
	
	var requestStruct = prepareRequest(ajaxZone, props);
	
	dojo.io.bind({
	    method: "POST",
		    url: window.location,
		    content: props, 
		    load: function(type, data, evt) {
		    var zones = dj_global.g_zones;
		    var subviews = new Array();
		    var i = 0;
		    // get the DOM elements for the subviews
		    for (i = 0; i < zones.length; i++) {
			subviews[i] = 
			    window.document.getElementById(zones[i].substring(1));
		    }
		    // get the processing contexts
		    var pCtxts = 
			data.getElementsByTagName("processing-context");
		    var postInstallHook = null;
		    for (i = 0; i < zones.length; i++) {
			subviews[i].innerHTML = pCtxts[i].childNodes[0].data;
			postInstallHook = 
			    pCtxts[i].getAttribute("postInstallHook");
			// If the processing context had a postInstallHook...
			if (typeof postInstallHook != 'undefined') {
			    postInstallHook = dj_global[postInstallHook];
			    // which identifies a globally scoped function...
			    if (typeof postInstallHook == 'function') {
				// call the function.
				postInstallHook(subviews[i], 
						subviews[i].innerHTML);
			    }
			}
			subviews[i].isAjaxified = null;
		    }
		},
		    mimetype: "text/xml"
		    });
	
    };
    
    dojo.event.connect("around", element, eventType, handler, "aroundHandler");
}

function takeActionAndTraverseTree(target, element, action, eventType, eventHook, postInstallHook) {
    var takeAction = false;
    var inspectElement = target.getAttribute("inspectElementHook");

    // If the user defined an "inspectElement" function, call it.
    if (null != (inspectElement = dj_global[inspectElement])) {
      takeAction = inspectElement(element);
    }
    // If the function returned false or null, or was not defined...
    if (null == takeAction || !takeAction) {
      // if this element has a handler for the eventType
      if (null != element[eventType] &&
  	  null != element.getAttribute(eventType)) {
        takeAction = true;
      }
    }
    if (takeAction) {
	// take the action on this element.
	action(target, element, eventType, eventHook);
    }
    if (element.hasChildNodes()) {
	for (var i = 0; i < element.childNodes.length; i++) {
	    takeActionAndTraverseTree(target, element.childNodes[i], action, 
				      eventType, eventHook);
	}
    }
    return false;
}

function prepareRequest(ajaxZone, extraParams) {
    var stateFieldName = "javax.faces.ViewState";
    var stateElements = window.document.getElementsByName(stateFieldName);
    // In the case of a page with multiple h:form tags, there will be
    // multiple instances of stateFieldName in the page.  Even so, they
    // all have the same value, so it's safe to use the 0th value.
    var stateValue = stateElements[0].value;
    // We must carefully encode the value of the state array to ensure
    // it is accurately transmitted to the server.  The implementation
    // of encodeURI() in mozilla doesn't properly encode the plus
    // character as %2B so we have to do this as an extra step.
    var uriEncodedState = encodeURI(stateValue);
    var rexp = new RegExp("\\+", "g");
    var encodedState = uriEncodedState.replace(rexp, "\%2B");
    // A truly robust implementation would discern the form number in
    // which the element named by "clientId" exists, and use that as the
    // index into the forms[] array.
    var formName = window.document.forms[0].id;
    // build up the post data
    extraParams[stateFieldName] = encodedState;
    extraParams[formName] = formName;
    if (null != ajaxZone.id) {
	extraParams[ajaxZone.id] = ajaxZone.id;
    }
}

/**
 * If the "nodeName" property of argument "element" contains the string
 * "input" (case-insensitive), extract the name (or id) and value of
 * that element and the corresponding value and store it in the "props"
 * associative array.  Otherwise, recurse over the children of
 * "element".
 */

function collectParamsFromInputChildren(element, props) {
    collectParamsFromChildrenOfType(element, "input", props);
    return;
}

/**
 * If the "nodeName" property of argument "element" contains the string
 * argument "nodeName" (case-insensitive), extract the name (or id) and
 * value of that element and the corresponding value and store it in the
 * "props" associative array.  Otherwise, recurse over the children of
 * "element".
 */
function collectParamsFromChildrenOfType(element, nodeName, props) {
    var elementNodeName = element.nodeName;
    var name = null;
    var i = 0;
    var result = null;
    if (null != elementNodeName) {
	if (-1 != elementNodeName.toLowerCase().indexOf(nodeName)) {
	    if (null == (name = element.name)) {
		name = element.id;
	    }
	    if (null != name) {
		props[name] = element.value;
	    }
	}
    }
    if (element.hasChildNodes()) {
	for (i = 0; i < element.childNodes.length; i++) {
	    collectParamsFromChildrenOfType(element.childNodes[i], 
					    nodeName, props);
	}
    }
    return;
}