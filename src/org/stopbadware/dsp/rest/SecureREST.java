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
	
	protected DBHandler getDBH() {
		Subject subject = AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
		return new DBHandler(subject);
	}

}
