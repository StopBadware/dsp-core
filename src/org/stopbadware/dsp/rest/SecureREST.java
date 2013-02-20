package org.stopbadware.dsp.rest;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.sec.AuthAuth;

public class SecureREST {
	
	@Context UriInfo uri;
	@Context HttpHeaders httpHeaders;
	protected Subject subject = AuthAuth.getEmptySubject();
	
	protected DBHandler getDBH() {
//		subject = AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
		subject = AuthAuth.getSubject(null, null);
		if (subject.isAuthenticated()) {
			return new DBHandler(subject);
		} else {
			return null;
		}
	}

}
