package org.stopbadware.dsp.rest;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.EventReports;

@Path("/add")
public class Add {
	
	private static DBHandler dbh = new DBHandler();
	private static final Logger LOG = LoggerFactory.getLogger(Add.class);
	
	@GET
	@Path("/timeoflast/{source}")
	public String getLastReportedTime(@PathParam("source") String source) {
		String foo = "AOK-"+source;
		//TODO: DATA-51 get time of last source from db
		return foo;
	}
	
	@POST
	@Path("/events")
	@Consumes(MediaType.APPLICATION_JSON)
	public String addEvents(String data) {
		System.out.println(data);	//DELME: DATA-51
		int numWroteToDB = 0;
		ObjectMapper mapper = new ObjectMapper();
		EventReports imports = null;
		try {
			imports = mapper.readValue(data, EventReports.class);
			System.out.println(imports.getReports().size());	//DELME: DATA-51
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		
		if (imports != null) {
			long age = (System.currentTimeMillis() / 1000) - imports.getTime();
			LOG.info("Received import with timestamp {}, ({} seconds ago)", imports.getTime(), age);
			if (age > 3600) {
				LOG.warn("Import timestamp is more than an hour old");
			}
			Set<Map<String, Object>> reports = imports.getReports(); 
			if (reports != null) {
				if (imports.getSize() == reports.size()) {
					LOG.info("{} event reports to write", imports.getSize());
					numWroteToDB = dbh.addEventReports(reports);
					LOG.info("{} successful write attempts", numWroteToDB);
				} else {
					LOG.error("Indicated report size of {} does not match number of reports unmarshalled {}, aborting imort", imports.getSize(), reports.size());
				}
			} else {
				LOG.error("Reports field is null");
			}
		} else {
			LOG.error("Add events called but no valid ImportContainer could be mapped from data");
		}

		return "AOK-"+numWroteToDB;
	}

}
