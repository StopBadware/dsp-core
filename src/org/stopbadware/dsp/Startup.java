package org.stopbadware.dsp;

import java.io.File;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.apache.catalina.startup.Tomcat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Startup implements ServletContextListener {
	
	private static final String ASTERISK_LINE = "**********************************************";
	private static final Logger LOG = LoggerFactory.getLogger(Startup.class);
	
    public static void main(String[] args) throws Exception {

        String webappDirLocation = "WebContent/";
        Tomcat tomcat = new Tomcat();

        //The port that we should run on can be set into an environment variable
        //Look for that variable and default to 8080 if it isn't there.
        String webPort = System.getenv("PORT");
        if(webPort == null || webPort.isEmpty()) {
            webPort = "8080";
        }

        tomcat.setPort(Integer.valueOf(webPort));

        tomcat.addWebapp("/", new File(webappDirLocation).getAbsolutePath());
        System.out.println("configuring app with basedir: " + new File("./" + webappDirLocation).getAbsolutePath());

        tomcat.start();
        tomcat.getServer().await();  
    }

	@Override
	public void contextInitialized(ServletContextEvent arg0) {
		LOG.info(ASTERISK_LINE);
		LOG.info("*******SBW DSP DB-WRAPPER / API STARTED*******");	
		LOG.info(ASTERISK_LINE);
	}
	
	@Override
	public void contextDestroyed(ServletContextEvent arg0) {
		LOG.info(ASTERISK_LINE);
		LOG.info("*******SBW DSP DB-WRAPPER / API STOPPED*******");	
		LOG.info(ASTERISK_LINE);
	}

}
