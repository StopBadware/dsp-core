package org.stopbadware.dsp.rest;

import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.AuthAuth;

@Path("/events")
public class FindEventReports {
	
	@Context HttpHeaders httpHeaders;
	private static DBHandler dbh = new DBHandler();
	
	@GET
	@Path("/test")
	public String secTest(ServletRequest request) {	//DELME: DATA-54 auth test method
		String path = "";
		if (request instanceof HttpServletRequest) {
			HttpServletRequest req = (HttpServletRequest) request;
			path = req.getPathTranslated();
		}
		if (AuthAuth.authenticated(httpHeaders, path)) {
			System.out.println("AUTH SUCCESS");
		} else {
			System.out.println("AUTH FAIL");
		}
		return "AOK";
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults findSince(@PathParam("param") String sinceTime) {
		return dbh.testFind(Long.valueOf(sinceTime));	//TODO: DATA-53 replace
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public TimeOfLast getLastReportedTime(@PathParam("source") String source) {
		return dbh.getTimeOfLast(source);
	}
}
