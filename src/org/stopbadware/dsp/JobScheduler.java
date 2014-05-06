package org.stopbadware.dsp;

import io.iron.ironmq.Client;
import io.iron.ironmq.Cloud;
import io.iron.ironmq.Queue;

import java.io.IOException;

import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;
import static org.quartz.SimpleScheduleBuilder.repeatHourlyForever;	

public class JobScheduler {

	private static final String IRON_PROJECT_ID = System.getenv("IRON_MQ_PROJECT_ID");
	private static final String IRON_TOKEN = System.getenv("IRON_MQ_TOKEN");
	private static final Cloud IRON_CLOUD = Cloud.ironAWSUSEast;
	private static final Logger LOG = LoggerFactory.getLogger(JobScheduler.class);
			
	public static void main(String[] args) throws Exception {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		
		JobDetail importer = newJob(Import.class).build();
		int impHours = (System.getenv("SBW_IMPORT_HOURS") != null) ? Integer.valueOf(System.getenv("SBW_IMPORT_HOURS")) : 0;
		if (impHours > 0) {
			Trigger importTrigger = newTrigger().startNow().withSchedule(repeatHourlyForever(impHours)).build();
			scheduler.scheduleJob(importer, importTrigger);
		}
		
		JobDetail resolver = newJob(Resolve.class).build();
		int resHours = (System.getenv("SBW_RESOLVE_HOURS") != null) ? Integer.valueOf(System.getenv("SBW_RESOLVE_HOURS")) : 0;
		if (resHours > 0) {
			Trigger resolverTrigger = newTrigger().startNow().withSchedule(repeatHourlyForever(resHours)).build();
			scheduler.scheduleJob(resolver, resolverTrigger);
		}
	}
	
	public static class Import implements Job {

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			beginImporting();
		}
		
		private void beginImporting() {
			if (IRON_PROJECT_ID != null && IRON_TOKEN != null) {
				Client client = new Client(IRON_PROJECT_ID, IRON_TOKEN, IRON_CLOUD);
				Queue queue = client.queue("import_queue");
				try {
					queue.push("all");
					LOG.info("Added begin importing request to queue");
				} catch (IOException e) {
					LOG.error("Unable to add import request for all to queue: {}", e.getMessage());
				}
			}
		}
		
	}
	
	public static class Resolve implements Job {
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			beginResolving();
		}
		
		private void beginResolving() {
			if (IRON_PROJECT_ID != null && IRON_TOKEN != null) {
				Client client = new Client(IRON_PROJECT_ID, IRON_TOKEN, IRON_CLOUD);
				Queue queue = client.queue("resolve_queue");
				try {
					queue.push("DSP");
					LOG.info("Added begin resolving request to queue");
				} catch (IOException e) {
					LOG.error("Unable to add resolve request to queue: {}", e.getMessage());
				}
			}
		}
		
	}

}
