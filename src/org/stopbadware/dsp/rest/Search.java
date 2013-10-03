package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.data.DbHandler.SearchType;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.Response;

@Path("/search")
public class Search extends SecureREST {
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@PathParam("param") String searchFor) {
		DbHandler dbh = getDBH();
		if (dbh != null) {
			MultivaluedMap<String, String> params = uri.getQueryParameters();
			try {
				switch (searchFor) {
				case "events":
					return dbh.search(SearchType.EVENT_REPORT, params);
				case "hosts":
					return dbh.search(SearchType.HOST, params);
				case "ips":
					return dbh.search(SearchType.IP, params);
				case "asns":
					return dbh.search(SearchType.AS, params);
				default:
					break;
				}
				return httpResponseCode(NOT_FOUND);
			} catch (SearchException e) {
				return new Error(e.getCode(), e.getMessage());
			}
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}

}
