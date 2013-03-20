package org.stopbadware.dsp;

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
		
		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			LOG.debug("BEGINNING RESOLVER");	//DELME: DATA-66
		}
	}

}
