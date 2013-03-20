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
import static org.quartz.SimpleScheduleBuilder.repeatSecondlyForever;

public class ImportScheduler {

	private static final Logger LOG = LoggerFactory.getLogger(ImportScheduler.class);
			
	public static void main(String[] args) throws Exception {
		Scheduler scheduler = StdSchedulerFactory.getDefaultScheduler();
		scheduler.start();
		String[] sources = {"mdl"};
		for (String source : sources) {
			JobDetail job = newJob(Import.class).build();
			Trigger trigger = newTrigger().startNow().withSchedule(repeatSecondlyForever(5)).build();
			scheduler.scheduleJob(job, trigger);
		}
	}
	
	public static class Import implements Job {
	
		private String source = "";
		
		public Import(String source) {
			this.source = source;
		}

		@Override
		public void execute(JobExecutionContext context) throws JobExecutionException {
			LOG.debug("BEGINNING IMPORT OF {}", source);	//DELME: DATA-66
		}
	}

}
