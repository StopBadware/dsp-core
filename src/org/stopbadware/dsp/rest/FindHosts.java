package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.ResolverRequest;
import org.stopbadware.dsp.json.Response;

@Path("/hosts")
public class FindHosts extends SecureRest {

	@GET
	@Path("/blacklisted/now")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince() {
		DbHandler dbh = getDbh();
		return (dbh != null) ? new ResolverRequest(dbh.getCurrentlyBlacklistedHosts()) : httpResponseCode(FORBIDDEN);
	}
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHost(@PathParam("param") String host) {
		DbHandler dbh = getDbh();
		return (dbh != null) ? dbh.findHost(host) : httpResponseCode(FORBIDDEN);
	}
	
}
