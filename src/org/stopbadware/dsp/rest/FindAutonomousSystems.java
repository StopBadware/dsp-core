package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.Error;

@Path("/asns")
public class FindAutonomousSystems extends SecureREST {
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getAS(@PathParam("param") String asn) {
		DbHandler dbh = getDBH();
		try {
			return (dbh != null) ? dbh.findAS(Integer.valueOf(asn)) : httpResponseCode(FORBIDDEN);
		} catch (NumberFormatException e) {
			return new Error(Error.BAD_FORMAT, "valid Autonomous System number not provided");
		}
	}

}
