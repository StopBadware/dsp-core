package org.stopbadware.dsp.rest;

import java.net.HttpURLConnection;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.sec.AuthAuth;

/**
 * Super-class ALL other classes in org.stopbadware.dsp.rest should extend. This
 * class provides for retrieval of a DBHandler instance after authenticating   
 */
public abstract class SecureRest {
	
	@Context UriInfo uri;
	@Context HttpHeaders httpHeaders;
	protected static final int OK = HttpURLConnection.HTTP_OK;
	protected static final int BAD_REQUEST = HttpURLConnection.HTTP_BAD_REQUEST;
	protected static final int FORBIDDEN = HttpURLConnection.HTTP_FORBIDDEN;
	protected static final int NOT_FOUND = HttpURLConnection.HTTP_NOT_FOUND;
	protected static final int INT_ERROR = HttpURLConnection.HTTP_INTERNAL_ERROR;
	
	/**
	 * Instantiates and returns a DBHandler instance
	 * @return a DBHandler instance associated with an authenticated subject,
	 * or null if the subject is not authenticated
	 */
	protected DbHandler getDbh() {
		Subject subject = getSubject();
		if (subject != null && subject.isAuthenticated()) {
			return new DbHandler(subject);
		} else {
			return null;
		}
	}
	
	/**
	 * Creates and returns a Shiro Subject from the
	 * required data in the URI and headers
	 * @return returns an authenticated subject, or null if authentication fails 
	 */
	protected Subject getSubject() {
		return AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
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
