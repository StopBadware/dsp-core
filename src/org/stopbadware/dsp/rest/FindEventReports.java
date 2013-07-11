package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DBHandler;
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
		Response response = null;
		DBHandler dbh = getDBH();
		if (dbh != null) {
			long time = 0L;
			try {
				time = Long.valueOf(sinceTime);
				response = dbh.findEventReportsSince(time);
				if (response == null) {
					response = httpResponseCode(FORBIDDEN);
				}
			} catch (NumberFormatException e) {
				response = new Error(Error.BAD_FORMAT, "Invalid timestamp to retrieve reports since");
			}
		} else {
			response = httpResponseCode(FORBIDDEN);
		}
		return response;
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastReportedTime(@PathParam("source") String source) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return dbh.getTimeOfLast(source);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
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
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return dbh.getEventReportsStats(source);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@GET
	@Path("/search")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search() {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			MultivaluedMap<String, String> params = uri.getQueryParameters();
			try {
				return dbh.eventReportSearch(params);
			} catch (SearchException e) {
				return new Error(e.getCode(), e.getMessage());
			}
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}	
}
