package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public abstract class MongoDB {
	
	private static final Logger LOG = LoggerFactory.getLogger(MongoDB.class);
	private static Mongo m = null;
	private static DB db = null;	
//	private static final String PRODUCTION_DB = "clearinghouse";
//	private static final String STAGING_DB = "stagedb";
	private static final String TESTING_DB = "testdb";
	private static final String DEVELOPMENT_DB = "devdb";

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";	

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	/**
	 * Switches the database for use in unit testing
	 */
	public static void switchToTestDB() {
		db = m.getDB(TESTING_DB);
	}
	
	/**
	 * Gets the database object associated with this instance
	 * @return MongoDB database object
	 */
	public static DB getDB() {
		if (db == null) {
			try {
				m = new Mongo();
				db = m.getDB(DEVELOPMENT_DB);
			} catch (UnknownHostException | MongoException e) {
				LOG.error("Unable to access database:\t{}", e.getMessage());
			}
		}
		return db;
	}	
}
