package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;

@Path("/events")
public class FindEventReports extends SecureREST {
	
	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	public Object secTest() {	//DELME: DATA-54 auth test method
		DBHandler dbh = getDBH();
		if (dbh != null) {
			for (String s : org.stopbadware.dsp.sec.AuthAuth.getAuthLevels(subject)) {
				System.out.println(s);
			}
			System.out.println("(200) AUTH SUCCESS");
			return new String("AOK");
		} else {
			System.out.println("(403) AUTH FAIL");
			return new String("403");
		}
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults findSince(@PathParam("param") String sinceTime) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return dbh.testFind(Long.valueOf(sinceTime));
		} else {
			System.out.println("(403) AUTH FAIL");	//TODO: DATA-54 return 403 SearchResults
			return null;
		}
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public TimeOfLast getLastReportedTime(@PathParam("source") String source) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return dbh.getTimeOfLast(source);
		} else {
			System.out.println("(403) AUTH FAIL");	//TODO: DATA-54 return 403 TimeOfLast
			return null;
		}
	}
}
