package org.stopbadware.dsp.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.sec.AuthAuth;

/**
 * Super-class ALL other classes in org.stopbadware.dsp.rest should extend. This
 * class provides for retrieval of a DBHandler instance after authenticating   
 */
public abstract class SecureREST {
	
	@Context UriInfo uri;
	@Context HttpHeaders httpHeaders;
	protected Subject subject = AuthAuth.getEmptySubject();
	
	/**
	 * Instantiates a DBHandler instance with a Shiro Subject created from
	 * the required data found in the HTTP headers and the URI
	 * @return a DBHandler instance with an authenticated subject, or null
	 * if the authentication failed
	 */
	protected DBHandler getDBH() {
		subject = AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
		if (subject.isAuthenticated()) {
			return new DBHandler(subject);
		} else {
			return null;
		}
	}

}
