package org.stopbadware.dsp.rest;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;

import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.CleanReports;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.EventReports;
import org.stopbadware.dsp.json.ResolverRequest;
import org.stopbadware.dsp.json.ResolverResults;
import org.stopbadware.dsp.json.Response;

@Path("/add")
public class Add extends SecureREST {
	
	private static final Logger LOG = LoggerFactory.getLogger(Add.class);
	//TODO: DATA-54 handle 403 via status code
	@POST
	@Path("/events")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEvents(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processImports(data, dbh);
			return new String("200");
		} else {
			return new String("403");
		}
	}
	
	@POST
	@Path("/clean")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response markClean(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processMarkClean(data, dbh);
			return new String("200");
		} else {
			return new String("403");
		}
	}
	
	@POST
	@Path("/resolve/start")
	public Response startResolver() {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			beginResolving(dbh);
			return new String("200");
		} else {
			return new String("403");
		}
	}
	
	@POST
	@Path("/resolved")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addResolved(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processResolved(data, dbh);
			return new String("200");
		} else {
			return new String("403");
		}
	}
	
	private void processImports(String data, DBHandler dbh) {
		int numWroteToDB = 0;
		ObjectMapper mapper = new ObjectMapper();
		EventReports imports = null;
		try {
			imports = mapper.readValue(data, EventReports.class);
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		
		if (imports != null) {
			long age = (System.currentTimeMillis() / 1000) - imports.getTime();
			LOG.info("Received import with timestamp {}, ({} seconds ago)", imports.getTime(), age);
			if (age > 3600) {
				LOG.warn("Import timestamp is more than an hour old");
			}
			Set<ERWrapper> reports = imports.getReports(); 
			if (reports != null) {
				if (imports.getSize() == reports.size()) {
					if (imports.isDifferentialBlacklist()) {
						processClean(imports, dbh);
					}
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
			LOG.error("Add events called but no valid EventReports could be mapped from data");
		}
	}
	
	private void processClean(EventReports er, DBHandler dbh) {
		LOG.info("Updating blacklist flags");
		int numCleaned = dbh.updateBlacklistFlagsFromDirtyReports(er.getSource(), er.getTime(), er.getReports());
		LOG.info("{} events removed from blacklist", numCleaned);
	}
	
	private void processMarkClean(String data, DBHandler dbh) {
		int numCleaned = 0;
		ObjectMapper mapper = new ObjectMapper();
		CleanReports clean = null;
		try {
			clean = mapper.readValue(data, CleanReports.class);
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		
		if (clean != null) {
			Set<String> cleanSet = clean.getClean();
			if (cleanSet != null) {
				if (clean.getSize() == cleanSet.size() && clean.getSource() != null) {
					LOG.info("{} total clean events for {}", clean.getSize(), clean.getSource());
					numCleaned = dbh.updateBlacklistFlagsFromCleanHosts(clean.getSource(), clean.getTime(), cleanSet);
					LOG.info("{} events removed from blacklist", numCleaned);
				} else {
					LOG.error("Indicated size of {} does not match number of clean events unmarshalled {}, aborting imort", clean.getSize(), cleanSet.size());
				}
			} else {
				LOG.error("Clean field is null");
			}
		} else {
			LOG.error("Add clean events called but no valid CleanReports could be mapped from data");
		}
	}
	
	private void beginResolving(DBHandler dbh) {
		ResolverRequest rr = new ResolverRequest(dbh.getCurrentlyBlacklistedHosts());
		LOG.info("Sending {} hosts to Resolver", rr.getHosts().size());
		try {
			Socket socket = new Socket("127.0.0.1", 1811);	//TODO: DATA-50 replace
			PrintWriter out = new PrintWriter(socket.getOutputStream());
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(out, rr);
			socket.close();
			LOG.info("Hosts sent, connection to Resolver closed");
		} catch (IOException e) {
			LOG.error("Unable to establish connection to DSP API:\t{}", e.getMessage());
		}	
	}
	
	private void processResolved(String data, DBHandler dbh) {
		ObjectMapper mapper = new ObjectMapper();
		ResolverResults rr = null;
		try {
			rr = mapper.readValue(data, ResolverResults.class);
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		
		if (rr != null) {
			dbh.addIPsForHosts(rr.getHostToIPMappings());
			dbh.addASNsForIPs(rr.getIpToASMappings());
		}
	}
	
}
