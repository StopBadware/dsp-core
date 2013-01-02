package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;

@Path("/add")
public class Add {
	
	private static DBHandler dbh = new DBHandler();
	
	@GET
	@Path("/events")
	@Produces(MediaType.APPLICATION_JSON)
	public String renameMe() {
		return "AOK";
	}

}
