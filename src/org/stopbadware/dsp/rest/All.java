package org.stopbadware.dsp.rest;

import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.data.SearchResults;

@Path("/all")
public class All {
	
	private static DBHandler dbh = new DBHandler();
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults test(@PathParam("param") String testString) {
		/*Delme d = new Delme();
		d.setA("all data");
		d.setB(testString);
		return d;*/
		
		return dbh.testFind(Long.valueOf(testString));
	}
}
