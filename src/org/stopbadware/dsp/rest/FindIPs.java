package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.Response;

@Path("/ips")
public class FindIPs extends SecureREST {
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHost(@PathParam("param") String ip) {
		DbHandler dbh = getDBH();
		return (dbh != null) ? dbh.findIP(ip) : httpResponseCode(FORBIDDEN); 
	}

}
