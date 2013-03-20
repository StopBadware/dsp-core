package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.ResolverRequest;
import org.stopbadware.dsp.json.Response;

@Path("/hosts")
public class FindHosts extends SecureREST {

	private static final Logger LOG = LoggerFactory.getLogger(FindHosts.class);
			
	@GET
	@Path("/blacklisted/now")
	@Produces(MediaType.APPLICATION_JSON)
	public Response findSince() {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			return new ResolverRequest(dbh.getCurrentlyBlacklistedHosts());
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
}
