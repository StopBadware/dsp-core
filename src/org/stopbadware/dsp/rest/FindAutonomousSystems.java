package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.Response;

@Path("/asns")
public class FindAutonomousSystems extends SecureREST {
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHost(@PathParam("param") String asn) {
		DBHandler dbh = getDBH();
//		return (dbh != null) ? dbh.findIP(ip) : httpResponseCode(FORBIDDEN);
		//TODO: DATA-103 find by ASN
		return httpResponseCode(OK);
	}

}
