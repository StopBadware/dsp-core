package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.AuthAuth;

@Path("/events")
public class FindEventReports {
	
	@Context UriInfo uri;
	@Context HttpHeaders httpHeaders;
	private DBHandler dbh = null;
	
	@GET
	@Path("/test")
	public String secTest() {	//DELME: DATA-54 auth test method
		String path = uri.getRequestUri().toString();
		Subject subject = AuthAuth.authenticated(httpHeaders, path);
		if (subject.isAuthenticated()) {
			dbh = new DBHandler(subject.getPrincipal().toString());
			System.out.println(subject.getPrincipal().toString());
			System.out.println(subject.toString());
			System.out.println(subject.isPermitted("foo"));
			System.out.println("AUTH SUCCESS");
			dbh.test1();
		} else {
			dbh = new DBHandler();
//			System.out.println(subject.getPrincipal().toString());
			System.out.println(subject.toString());
			System.out.println(subject.isAuthenticated());
			System.out.println(subject.isPermitted("foo"));
			System.out.println("AUTH FAIL");
			dbh.test2();
		}
		return "AOK";
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults findSince(@PathParam("param") String sinceTime) {
		dbh = new DBHandler();	//TODO: DATA-50 fix call
		return dbh.testFind(Long.valueOf(sinceTime));	//TODO: DATA-53 replace
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public TimeOfLast getLastReportedTime(@PathParam("source") String source) {
		dbh = new DBHandler();	//TODO: DATA-50 fix call
		return dbh.getTimeOfLast(source);
	}
}
