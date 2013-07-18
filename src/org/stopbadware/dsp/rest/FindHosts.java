package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.data.DBHandler.SearchType;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.ResolverRequest;
import org.stopbadware.dsp.json.Response;

@Path("/hosts")
public class FindHosts extends SecureREST {

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
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getHost(@PathParam("param") String sinceTime) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			//TODO: DATA-96 return host record
			return httpResponseCode(OK);
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
				return dbh.search(SearchType.HOST, params);
			} catch (SearchException e) {
				return new Error(e.getCode(), e.getMessage());
			}
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}	
}
