package org.stopbadware.dsp.rest;

import java.net.HttpURLConnection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.sec.AuthAuth;

/**
 * Super-class ALL other classes in org.stopbadware.dsp.rest should extend. This
 * class provides for retrieval of a DBHandler instance after authenticating   
 */
public abstract class SecureREST {
	
	@Context UriInfo uri;
	@Context HttpHeaders httpHeaders;
	protected Subject subject;
	protected static final int OK = HttpURLConnection.HTTP_OK;
	protected static final int FORBIDDEN = HttpURLConnection.HTTP_FORBIDDEN;
	
	/**
	 * Instantiates a DBHandler instance with a Shiro Subject created from
	 * the required data found in the HTTP headers and the URI
	 * @return a DBHandler instance with an authenticated subject, or null
	 * if the authentication failed
	 */
	protected DBHandler getDBH() {
		subject = AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
		if (subject != null && subject.isAuthenticated()) {
			return new DBHandler(subject);
		} else {
			return null;
		}
	}
	
	/**
	 * Returns a response with a blank message and specified HTTP status code, the
	 * status code's short description (i.e. 'Forbidden' for 403 or 'Not Found' for 404
	 * will be supplied by the application server when possible). This is a convenience
	 * method for throwing a new javax.ws.rsWebApplicationException with the 
	 * supplied status code
	 * @param status the HTTP status code that will be returned to the client
	 * @return HTTP response with specified code and short description (when possible)
	 */
	protected Response httpResponseCode(int status) {
		throw new WebApplicationException(status);
	}

}
