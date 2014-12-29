package org.stopbadware.dsp.rest;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import org.apache.shiro.subject.Subject;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DbHandler;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.CleanReports;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.EventReports;
import org.stopbadware.dsp.json.ResolverResults;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.sec.AuthAuth;
import org.stopbadware.lib.util.SHA2;

@Path("/add")
public class Add extends SecureRest {
	
	private static final Logger LOG = LoggerFactory.getLogger(Add.class);
	
	@POST
	@Path("/events")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addEvents(String data) {
		DbHandler dbh = getDbh();
		if (dbh != null) {
			Executors.newSingleThreadExecutor().execute(new AddEvents(data, dbh));
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@POST
	@Path("/{source}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response pushToApi(@PathParam("source") String dataSource, String data) {
		int status = OK;
		Subject subject = getSubject();
		if (subject != null && (subject.isAuthenticated())) {
			if (AuthAuth.subjectIsMemberOf(subject, dataSource)) {
				try {
					boolean sendSuccess = sendToImporter(dataSource, data);
					status = (sendSuccess) ? OK : BAD_REQUEST;
				} catch (IOException e) {
					LOG.error("Exception thrown sending data to Importer: {}", e.getMessage());
					status = INT_ERROR;
				}
			} else {
				status = NOT_FOUND;
			}
		} else {
			status = FORBIDDEN;
		}
		return httpResponseCode(status);
	}
	
	private boolean sendToImporter(String source, String data) throws IOException {
		URL url = new URL(System.getenv("SBW_IMP_HOST")+"/import/"+source);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		conn.setRequestMethod("POST");
		conn.setRequestProperty("Content-Type", "application/json");
		Map<String, String> authHeaders = createImporterAuthHeaders(url.getPath().toString());
		for (String key : authHeaders.keySet()) {
			conn.setRequestProperty(key, authHeaders.get(key));
		}
		conn.setDoOutput(true);
		OutputStream out = conn.getOutputStream();
		out.write(data.getBytes("UTF-8"));
		out.flush();
		out.close();
		int resCode = conn.getResponseCode();
		conn.disconnect();
		return resCode == 200;
	}
	
	private Map<String, String> createImporterAuthHeaders(String path) {
		Map<String, String> headers = new HashMap<>();
		String secret = (System.getenv("SBW_IMP_SECRET")!=null) ? System.getenv("SBW_IMP_SECRET") : "";
		String timestamp = String.valueOf(System.currentTimeMillis() / 1000);
		String signature = SHA2.get256(timestamp+secret);
		headers.put("SBW-IMP-Timestamp", timestamp);
		headers.put("SBW-IMP-Signature", signature);
		return headers;
	}
	
	@POST
	@Path("/clean")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response markClean(String data) {
		DbHandler dbh = getDbh();
		if (dbh != null) {
			processMarkClean(data, dbh);
			return httpResponseCode(OK);
		} else {
			return httpResponseCode(FORBIDDEN);
		}
	}
	
	@POST
	@Path("/resolved")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response addResolved(String data) {
		int status = FORBIDDEN;
		final DbHandler dbh = getDbh();
		if (dbh != null) {
			final ResolverResults results = mapResolved(data);
			if (results != null) {
				new Thread() {
					@Override
					public void run() {
						processResolverResults(results, dbh);
					}
				}.start();
				status = OK;
			} else {
				status = BAD_REQUEST;
			}
		}
		return httpResponseCode(status);
	}
	
	private void processClean(EventReports er, DbHandler dbh) {
		LOG.info("Updating blacklist flags");
		int numCleaned = dbh.updateBlacklistFlagsFromDirtyReports(er.getSource(), er.getTime(), er.getReports());
		LOG.info("{} events removed from blacklist", numCleaned);
	}
	
	private void processMarkClean(String data, DbHandler dbh) {
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
	
	/**
	 * Maps results from the IP/AS Resolver
	 * @param data JSON String containing the results of the resolver run
	 * @return a valid ResolverResults instance if the passed data was 
	 * successfully mapped to a ResolverResults object, otherwise null.
	 */
	private ResolverResults mapResolved(String data) {
		ObjectMapper mapper = new ObjectMapper();
		ResolverResults rr = null;
		try {
			rr = mapper.readValue(data, ResolverResults.class);
		} catch (IOException e) {
			LOG.error("Error parsing JSON:\t{}", e.getMessage());
		}
		return rr;
	}
	
	private void processResolverResults(ResolverResults rr, DbHandler dbh) {
		dbh.addIPsForHosts(rr.getHostToIPMappings());
		Map<Long, AutonomousSystem> ipAs = rr.getIpToASMappings();
		dbh.addAsnsForIps(ipAs);
		Set<AutonomousSystem> uniqueAutonomousSystems = new HashSet<>();
		uniqueAutonomousSystems.addAll(ipAs.values());
		dbh.addAutonmousSystems(uniqueAutonomousSystems);
	}
	
	private class AddEvents implements Runnable {
		
		private String data = null;
		private DbHandler dbh = null;
		
		public AddEvents(String data, DbHandler dbh) {
			this.data = data;
			this.dbh = dbh;
		}
		
		private void processImports() {
			int numWroteToDB = 0;
			ObjectMapper mapper = new ObjectMapper();
			EventReports imports = null;
			try {
				imports = mapper.readValue(data, EventReports.class);
			} catch (IOException e) {
				LOG.error("Error parsing JSON:\t{}", e.getMessage());
			}
			
			if (imports != null) {
				LOG.info("Received import with timestamp {} for '{}'", imports.getTime(), imports.getSource());
				Set<ERWrapper> reports = imports.getReports(); 
				if (reports != null) {
					if (imports.getSize() == reports.size()) {
						if (imports.isDifferentialBlacklist()) {
							processClean(imports, dbh);
						}
						LOG.info("{} event reports to write", imports.getSize());
						numWroteToDB = dbh.addEventReports(reports);
                        spawnThreadToAddIPs(reports);
						LOG.info("{} successful write attempts", numWroteToDB);
					} else {
						LOG.error("Indicated report size of {} does not match number of reports unmarshalled {}, aborting imort", imports.getSize(), reports.size());
					}
				} else {
					LOG.error("Reports field is null");
				}
			} else {
				LOG.warn("Add events called but no valid EventReports could be mapped from data");
			}
		}

		boolean threadSpawned = false;
		Object queueLock = new Object();
		Set<ERWrapper> combinedReports = new HashSet<>();

        private void spawnThreadToAddIPs(final Set<ERWrapper> reports) {
            LOG.info("Spawning thread to add IPs to {} reports.",reports.size());
			synchronized (queueLock) {
				if(!threadSpawned) {
					threadSpawned = true;
					combinedReports.addAll(reports);
					new Thread() {
						@Override
						public void run() {
							try {
								Thread.sleep(10000);
								synchronized (queueLock) {
									LOG.info("Thread executing, dbh = {}.", dbh);
									addIPsToEventReports(combinedReports, dbh);
									combinedReports.clear();
									threadSpawned = false;
								}
							} catch (InterruptedException e) {
								if(LOG.isErrorEnabled()) {
									String hosts = "";
									for (ERWrapper er : combinedReports) {
										hosts += er.getHost() + ", ";
									}
									LOG.error("IP lookup thread interrupted before "+hosts+" could be processed.", e);
								}
							}
						}
					}.start();
				}
			}
        }

        private void addIPsToEventReports(Set<ERWrapper> reports, DbHandler dbh) {
            Map<String,Set<Long>> hostIPMap = new HashMap<>();
            for(ERWrapper er: reports) {
                String host = er.getHost();
                if (!hostIPMap.containsKey(host)) {
                    Set<Long> ips = dbh.getIPsForHost(host);
                    hostIPMap.put(host, ips);
                    if (ips.size() > 0) {
                        LOG.debug("IPs {} will be added to event report {}", ips, er.getHost());
                    }
                }
            }
            for(String host: hostIPMap.keySet()) {
                dbh.addIPsToEventReports(host, hostIPMap.get(host));
            }
        }

        @Override
		public void run() {
			processImports();
		}
		
	}
	
}
