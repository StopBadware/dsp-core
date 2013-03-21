package org.stopbadware.dsp;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.lib.util.SHA2;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.repeatHourlyForever;	

public class ImportScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
			
	public static void main(String[] args) throws Exception {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		
		JobDetail importer = newJob(Import.class).build();
		Trigger importTrigger = newTrigger().startNow().withSchedule(repeatHourlyForever(1)).build();
		scheduler.scheduleJob(importer, importTrigger);
		
		JobDetail resolver = newJob(Resolve.class).build();
		Trigger resolverTrigger = newTrigger().startNow().withSchedule(repeatHourlyForever(12)).build();
		scheduler.scheduleJob(resolver, resolverTrigger);
	}
	
	public static class Import implements Job {

		private static String impHost = (System.getenv("SBW_IMP_HOST")!=null) ? System.getenv("SBW_IMP_HOST") : "http://127.0.0.1";
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			beginImporting();
		}
		
		private void beginImporting() {
			try {
				URL url = new URL(impHost+"/import/all/");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				Map<String, String> authHeaders = createImporterAuthHeaders(url.getPath().toString());
				for (String key : authHeaders.keySet()) {
					conn.setRequestProperty(key, authHeaders.get(key));
				}
				int resCode = conn.getResponseCode();
				if (resCode == 200) {
					LOG.info("Begin importing [all] request sent to Importer");
				} else {
					LOG.error("Unable to connect to Importer, received HTTP Status Code {}", resCode);
				}
			} catch (IOException e) {
				LOG.error("Unable to connect to Importer:\t{}", e.getMessage());
			}
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
	}
	
	public static class Resolve implements Job {
		
		private static String resHost = (System.getenv("SBW_RES_HOST")!=null) ? System.getenv("SBW_RES_HOST") : "http://127.0.0.1";
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			beginResolving();
		}
		
		private void beginResolving() {
			try {
				URL url = new URL(resHost+"/resolve/hosts/");
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("POST");
				Map<String, String> authHeaders = createResolverAuthHeaders(url.getPath().toString());
				for (String key : authHeaders.keySet()) {
					conn.setRequestProperty(key, authHeaders.get(key));
				}
				int resCode = conn.getResponseCode();
				if (resCode == 200) {
					LOG.info("Begin resolving request sent to Resolver");
				} else {
					LOG.error("Unable to connect to resolver, received HTTP Status Code {}", resCode);
				}
			} catch (IOException e) {
				LOG.error("Unable to connect to Resolver:\t{}", e.getMessage());
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
