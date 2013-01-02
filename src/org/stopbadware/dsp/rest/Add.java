package org.stopbadware.dsp.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;

@Path("/add")
public class Add {
	
	private static DBHandler dbh = new DBHandler();
	
	@POST
	@Path("/events")
//	@Consumes(MediaType.APPLICATION_JSON)	//TODO:DATA-42 revert
	@Produces(MediaType.APPLICATION_JSON)
	public String addEvents(String data) {
//		curl -d '' http://127.0.0.1:8080/clearinghouse/add/events
		System.out.println(data);	//DELME: DATA-42
		//TODO: DATA-42 put input data as HashSet<EventReports> via Jackson
		//TODO: DATA-42 send set to	dbh.addToEventReports() 
		//TODO: DATA-42 return number added
		return "AOK";
	}

}
