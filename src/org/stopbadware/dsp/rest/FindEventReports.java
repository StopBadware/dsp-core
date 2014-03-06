package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.RateLimitException;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.Error;

@Path("/events")
public class FindEventReports extends SecureRest {
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince(@PathParam("param") String sinceTime) {
		DbHandler dbh = getDbh();
		try {
			return (dbh != null) ? dbh.findEventReportsSince(sinceTime) : httpResponseCode(FORBIDDEN);
		} catch (SearchException e) {
			return new Error(e.getCode(), e.getMessage());
		} catch (RateLimitException e) {
			return rateLimitExceeded(e.getMessage());
		}
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastReportedTime(@PathParam("source") String source) {
		DbHandler dbh = getDbh();
		return (dbh != null) ? dbh.getTimeOfLast(source) : httpResponseCode(FORBIDDEN);
	}
	
	@GET
	@Path("/prefixes")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getParticipantPrefixes() {
		Response response = null;
		DbHandler dbh = getDbh();
		if (dbh != null) {
			response = dbh.getParticipantPrefixes();
		} else {
			response = httpResponseCode(FORBIDDEN);
		}
		return response;
	}
	
	@GET
	@Path("/stats/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStats(@PathParam("source") String source) {
		DbHandler dbh = getDbh();
		return (dbh != null) ? dbh.getEventReportsStats(source) : httpResponseCode(FORBIDDEN);
	}
	
	@GET
	@Path("/report/{uid}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response find(@PathParam("uid") String uid) {
		DbHandler dbh = getDbh();
		try {
			return (dbh != null) ? dbh.findEventReport(uid) : httpResponseCode(FORBIDDEN);
		} catch (SearchException e) {
			return new Error(e.getCode(), e.getMessage());
		}
	}	
}
