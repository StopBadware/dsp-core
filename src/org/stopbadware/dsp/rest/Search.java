package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.data.DbHandler.SearchType;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.SearchResults;

@Path("/search")
public class Search extends SecureRest {
	
	private static final Logger LOG = LoggerFactory.getLogger(Search.class);
	
	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response search(@PathParam("param") String searchFor) {
		DbHandler dbh = getDbh();
		if (dbh != null) {
			SearchResults sr = null;
			MultivaluedMap<String, String> params = uri.getQueryParameters();
			try {
				switch (searchFor) {
				case "events":
					sr = dbh.search(SearchType.EVENT_REPORT, params);
				case "hosts":
					sr = dbh.search(SearchType.HOST, params);
				case "ips":
					sr = dbh.search(SearchType.IP, params);
				case "asns":
					sr = dbh.search(SearchType.AS, params);
				default:
					break;
				}
				
				if (sr != null) {
					//TODO DATA-126 get query string
					LOG.info("{}:Returning {} results in {}ms for '{}'", sr.getCode(), sr.getCount(), sr.getDuration(), searchFor);
					return sr;
				} else {
					return httpResponseCode(NOT_FOUND);
				}
			} catch (SearchException e) {
				//TODO DATA-126 get query string
				LOG.warn("{}:Searching for '{}' resulted in: {}", e.getCode(), searchFor, e.getMessage());
				return new Error(e.getCode(), e.getMessage());
			}
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}

}
