package org.stopbadware.dsp.rest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;

@Path("/add")
public class Add {
	
	private static DBHandler dbh = new DBHandler();
	private static final Logger LOG = LoggerFactory.getLogger(Add.class);
	
	@POST
	@Path("/events")
	@Consumes(MediaType.APPLICATION_JSON)
	public String addEvents(String data) {
		System.out.println(data);	//DELME: DATA-51
		ObjectMapper mapper = new ObjectMapper();
		try {
			ReportContainer reports = mapper.readValue(data, ReportContainer.class);
			System.out.println(reports.reports.size());
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		//TODO: DATA-51 send set to	dbh.addToEventReports() 
		return "AOK";
	}
	
	public static class ReportContainer {
		public int size;
		public long time;
		public Set<Map<String, Object>> reports;
		public ReportContainer() {
			
		}
	}

}
