package org.stopbadware.dsp;

import java.util.TimeZone;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup implements ServletContextListener {
	
	private static final Logger LOG = LoggerFactory.getLogger(Startup.class);

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		TimeZone.setDefault(TimeZone.getTimeZone("GMT"));
		LOG.info("**********************************************");
		LOG.info("*******SBW DSP DB-WRAPPER / API STARTED*******");	
		LOG.info("**********************************************");
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		LOG.info("**********************************************");
		LOG.info("*******SBW DSP DB-WRAPPER / API STOPPED*******");	
		LOG.info("**********************************************");
	}

}
