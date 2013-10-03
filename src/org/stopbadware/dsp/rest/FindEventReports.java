package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.Error;

@Path("/events")
public class FindEventReports extends SecureREST {
	
	/*
	 * **********************************
	 * **************v0.2****************
	 * **********************************
	 */
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince(@PathParam("param") String sinceTime) {
		DbHandler dbh = getDBH();
		try {
			return (dbh != null) ? dbh.findEventReportsSince(Long.valueOf(sinceTime)) : httpResponseCode(FORBIDDEN);
		} catch (NumberFormatException e) {
			return new Error(Error.BAD_FORMAT, "Invalid timestamp to retrieve reports since");
		}
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastReportedTime(@PathParam("source") String source) {
		DbHandler dbh = getDBH();
		return (dbh != null) ? dbh.getTimeOfLast(source) : httpResponseCode(FORBIDDEN);
	}
	
	/*
	 * **********************************
	 * ***************v2*****************
	 * **********************************
	 */
	
	@GET
	@Path("/stats/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStats(@PathParam("source") String source) {
		DbHandler dbh = getDBH();
		return (dbh != null) ? dbh.getEventReportsStats(source) : httpResponseCode(FORBIDDEN);
	}
	
	@GET
	@Path("/report/{uid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response find(@PathParam("uid") String uid) {
		DbHandler dbh = getDBH();
		try {
			return (dbh != null) ? dbh.findEventReport(uid) : httpResponseCode(FORBIDDEN);
		} catch (SearchException e) {
			return new Error(e.getCode(), e.getMessage());
		}
	}	
}
