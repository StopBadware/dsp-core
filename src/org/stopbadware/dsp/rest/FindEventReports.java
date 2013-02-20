package org.stopbadware.dsp.rest;

import java.net.URI;

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
public class FindEventReports extends SecureREST {
	
//	@Context UriInfo uri;
//	@Context HttpHeaders httpHeaders;
//	private DBHandler dbh = null;
	
//	private DBHandler OLDgetDBH() {
//		Subject subject = AuthAuth.getSubject(httpHeaders, uri.getRequestUri());
//		DBHandler dbh = new DBHandler(subject);
//		if (subject.isAuthenticated()) {
//			status = 200;
////			dbh = new DBHandler(subject.getPrincipal().toString());
////			dbh = new DBHandler(subject.getPrincipal().toString());
//			System.out.println(subject.getPrincipal().toString());
//			System.out.println(subject.toString());
//			System.out.println(subject.isPermitted("foo"));
//			System.out.println("AUTH SUCCESS");
//			dbh.test1();
//		} else {
//			status = 403;
//			System.out.println("(403) AUTH FAIL");
//			//TODO: DATA-54 return 403
//		}
//		
//		return dbh;
//	}
	
	@GET
	@Path("/test")
	public String secTest() {	//DELME: DATA-54 auth test method
		DBHandler dbh = getDBH();
		if (subject.isAuthenticated() /*&& subject.isPermitted("foo")*/) {
			//TODO: DATA-54 do db stuff
			return "AOK";
		} else {
			//TODO: DATA-54 return 403
			System.out.println("(403) AUTH FAIL");
			return "403";
		}
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults findSince(@PathParam("param") String sinceTime) {
		DBHandler dbh = new DBHandler();	//TODO: DATA-50 fix call
		return dbh.testFind(Long.valueOf(sinceTime));	//TODO: DATA-53 replace
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public TimeOfLast getLastReportedTime(@PathParam("source") String source) {
		DBHandler dbh = new DBHandler();	//TODO: DATA-50 fix call
		return dbh.getTimeOfLast(source);
	}
}
