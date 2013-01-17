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
//		LOG.debug("SBW DSP Started");	//TODO: DATA-51 prettify
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
//		LOG.debug("SBW DSP Stopped");	//TODO: DATA-51 prettify
	}

}
