package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.Error;

@Path("/events")
public class FindEventReports extends SecureREST {
	
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
				response = dbh.testFind(time);
			} catch (NumberFormatException e) {
				response = new Error(400, "Bad Request: invalid timestamp to retreive reports since");
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
}
