package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.RateLimitException;
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
			String pathAndParams = uri.getPath() + params.toString();
			SearchType type = null;
			switch (searchFor) {
			case "events":
				type = SearchType.EVENT_REPORT;
				break;
			case "hosts":
				type = SearchType.HOST;
				break;
			case "ips":
				type = SearchType.IP;
				break;
			case "asns":
				type = SearchType.AS;
				break;
			default:
				break;
			}
			
			try {
				if (type != null) {
					sr = dbh.search(type, params);
				}
				if (sr != null) {
					int duration = sr.getDuration();
					if (duration > 200) {
						LOG.warn("{}:Fetching {} results took {}ms [SLOW] for '{}'", sr.getCode(), sr.getCount(), duration, pathAndParams);
					} else {
						LOG.info("{}:Fetching {} results took {}ms for '{}'", sr.getCode(), sr.getCount(), duration, pathAndParams);
					}
					return sr;
				} else {
					return httpResponseCode(NOT_FOUND);
				}
			} catch (SearchException e) {
				LOG.warn("{}:SearchException thrown for '{}': {}", e.getCode(), pathAndParams, e.getMessage());
				return new Error(e.getCode(), e.getMessage());
			} catch (RateLimitException e) {
				return rateLimitExceeded(e.getMessage());
			}
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}

}
