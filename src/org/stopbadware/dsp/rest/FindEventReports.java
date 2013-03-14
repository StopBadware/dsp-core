package org.stopbadware.dsp.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.sec.AuthAuth;

@Path("/events")
public class FindEventReports extends SecureREST {
	
	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	public Response test(Object data) {	//DELME: DATA-54 test method
//		http://127.0.0.1:5000/events/test/
//		DBHandler dbh = getDBH();
//		DBHandler dbh = new DBHandler(AuthAuth.getEmptySubject());
		LoggerFactory.getLogger(FindEventReports.class).debug("REC:{}", data.toString());
		LoggerFactory.getLogger(FindEventReports.class).debug("200 OK");
		return httpResponseCode(OK);
//		if (dbh != null) {
//			System.out.println("(200) AUTH SUCCESS");
//			return new String("AOK");
//		} else {
//			System.out.println("(403) AUTH FAIL");
//			return new String("403");
//		}
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince(@PathParam("param") String sinceTime) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return dbh.testFind(Long.valueOf(sinceTime));
		} else {
			return httpResponseCode(FORBIDDEN);
		}
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
