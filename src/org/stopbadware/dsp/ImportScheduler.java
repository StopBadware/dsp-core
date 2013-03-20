package org.stopbadware.dsp;

import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.json.ResolverRequest;
import org.stopbadware.lib.util.SHA2;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.repeatSecondlyForever;;	//TODO: DATA-66 change to hourly
//import static org.quartz.SimpleScheduleBuilder.repeatHourlyForever;	

public class ImportScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
			
	public static void main(String[] args) throws Exception {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		
		JobDetail importer = newJob(Import.class).build();
		Trigger importTrigger = newTrigger().startNow().withSchedule(repeatSecondlyForever(10)).build();
		scheduler.scheduleJob(importer, importTrigger);
		
		JobDetail resolver = newJob(Resolve.class).build();
		Trigger resolverTrigger = newTrigger().startNow().withSchedule(repeatSecondlyForever(12)).build();
		scheduler.scheduleJob(resolver, resolverTrigger);
	}
	
	public static class Import implements Job {
	
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			LOG.debug("BEGINNING IMPORTS");		//DELME: DATA-66
		}
	}
	
	public static class Resolve implements Job {
		
		private static String resHost = (System.getenv("RES_HOST")!=null) ? System.getenv("RES_HOST") : "http://127.0.0.1";
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			LOG.debug("BEGINNING RESOLVER");	//DELME: DATA-66
			beginResolving();
		}
		
		private void beginResolving() {
			try {
				URL url = new URL(resHost+"/resolve/begin/");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				Map<String, String> authHeaders = createResolverAuthHeaders(url.getPath().toString());
				for (String key : authHeaders.keySet()) {
					conn.setRequestProperty(key, authHeaders.get(key));
				}
//				conn.setDoOutput(true);
//				PrintStream out = new PrintStream(conn.getOutputStream());
//				ObjectMapper mapper = new ObjectMapper();
//				mapper.writeValue(out, rr);
				int resCode = conn.getResponseCode();
				if (resCode == 200) {
					LOG.info("Begin resolving request sent to Resolver");
				} else {
					LOG.error("Unable to connect to resolver, received HTTP Status Code {}", resCode);
				}
			} catch (IOException e) {
				LOG.error("Unable to connect to resolver:\t", e.getMessage());
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

}
