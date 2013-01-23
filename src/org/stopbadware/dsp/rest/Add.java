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

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;

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
		ImportContainer ic = null;
		try {
			ic = mapper.readValue(data, ImportContainer.class);
			System.out.println(ic.reports.size());	//DELME: DATA-51
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		
		if (ic != null) {
			long age = (System.currentTimeMillis() / 1000) - ic.time;
			LOG.info("Received import with timestamp {}, ({} seconds ago)", ic.time, age);
			if (age > 3600) {
				LOG.warn("Import timestamp is more than an hour old");
			}
			if (ic.reports != null) {
				if (ic.size == ic.reports.size()) {
					LOG.info("{} event reports to write", ic.size);
					numWroteToDB = dbh.addEventReports(ic.reports);
					LOG.info("{} successful write attempts", numWroteToDB);
				} else {
					LOG.error("Indicated report size of {} does not match number of reports unmarshalled {}, aborting imort", ic.size, ic.reports.size());
				}
			} else {
				LOG.error("Reports field is null");
			}
		} else {
			LOG.error("Add events called but no valid ImportContainer could be mapped from data");
		}

		return "AOK-"+numWroteToDB;
	}
	
	@JsonIgnoreProperties(ignoreUnknown = true)
	private static class ImportContainer {
		public int size;
		public long time;
		public Set<Map<String, Object>> reports;
	}

}
