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
public class FindEventReports {
	
	private static DBHandler dbh = new DBHandler();
	
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