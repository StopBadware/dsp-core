package org.stopbadware.dsp.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class DBHandler {
	
	private DB db;
	private DBCollection eventReportColl;
	private DBCollection hostColl;
	private DBCollection ipColl;
	private DBCollection asColl;
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	
	public DBHandler() {
		LOG.debug("test log");
	}

}
