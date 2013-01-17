package org.stopbadware.dsp.rest;

import javax.ws.rs.POST;
import javax.ws.rs.Path;

import org.stopbadware.dsp.data.DBHandler;

@Path("/add")
public class Add {
	
	private static DBHandler dbh = new DBHandler();
	
	@POST
	@Path("/events")
//	@Consumes(MediaType.APPLICATION_JSON)	//TODO: DATA-51 revert or remove
	public String addEvents(String data) {
//		curl -d '' http://127.0.0.1:8080/clearinghouse/add/events
		System.out.println(data);	//DELME: DATA-51
		//TODO: DATA-51 put input data as HashSet<Map<String, Object>> via Jackson
		//TODO: DATA-51 send set to	dbh.addToEventReports() 
		return "AOK";
	}

}
