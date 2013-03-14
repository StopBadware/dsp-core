package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.Response;

@Path("/events")
public class FindEventReports extends SecureREST {
	
	private static final Logger LOG = LoggerFactory.getLogger(FindEventReports.class);	//DELME: DATA-41	
			
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince(@PathParam("param") String sinceTime) {
		DBHandler dbh = getDBH();
		LOG.debug("RX:{}", sinceTime);						//DELME: DATA-41
		if (dbh != null) {			
			return httpResponseCode(OK);					//DELME: DATA-41
//			return dbh.testFind(Long.valueOf(sinceTime));	//TODO: DATA-41 revert
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getLastReportedTime(@PathParam("source") String source) {
		DBHandler dbh = getDBH();
		LOG.debug("RX:{}", source);				//DELME: DATA-41
		if (dbh != null) {
			return httpResponseCode(OK);		//DELME: DATA-41
//			return dbh.getTimeOfLast(source);	//TODO: DATA-41 revert
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
}
