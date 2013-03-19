package org.stopbadware.dsp.rest;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
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
import org.stopbadware.lib.util.SHA2;

@Path("/add")
public class Add extends SecureREST {
	
	private static final Logger LOG = LoggerFactory.getLogger(Add.class);
	protected static String resHost = (System.getenv("RES_HOST")!=null) ? System.getenv("RES_HOST") : "http://127.0.0.1";	
	
	@POST
	@Path("/events")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEvents(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processImports(data, dbh);
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@POST
	@Path("/clean")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response markClean(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processMarkClean(data, dbh);
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@POST
	@Path("/resolve/start")
	public Response startResolver() {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			beginResolving(dbh);
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@POST
	@Path("/resolved")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addResolved(String data) {
		DBHandler dbh = getDBH();
		if (dbh != null) {
			processResolved(data, dbh);
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
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
		try {
			URL url = new URL(resHost+"/resolve/hosts/");
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("POST");
			conn.setRequestProperty("Content-Type", "application/json");
			Map<String, String> authHeaders = createResolverAuthHeaders(url.getPath().toString());
			for (String key : authHeaders.keySet()) {
				conn.setRequestProperty(key, authHeaders.get(key));
			}
			conn.setDoOutput(true);
			PrintStream out = new PrintStream(conn.getOutputStream());
			ObjectMapper mapper = new ObjectMapper();
			mapper.writeValue(out, rr);
			int resCode = conn.getResponseCode();
			if (resCode == 200) {
				LOG.info("{} hosts sent to Resolver", rr.getHosts().size());
			} else {
				LOG.error("Unable to send hosts to resolver, received HTTP Status Code {}", resCode);
			}
		} catch (IOException e) {
			LOG.error("Unable to send hosts to resolver:\t", e.getMessage());
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
	
	private Map<String, String> createResolverAuthHeaders(String path) {
		Map<String, String> headers = new HashMap<>();
		String secret = (System.getenv("SBW_RES_SECRET")!=null) ? System.getenv("SBW_RES_SECRET") : "";
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		String signature = SHA2.get256(timestamp+secret);
		headers.put("SBW-RES-Timestamp", timestamp);
		headers.put("SBW-RES-Signature", signature);
		return headers;
	}
	
}
