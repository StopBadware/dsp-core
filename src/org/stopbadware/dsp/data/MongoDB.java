package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;

public class MongoDB {
	
	private static final Logger LOG = LoggerFactory.getLogger(MongoDB.class);
	private static MongoDB instance = null;
	private Mongo m;
	private DB db;	
//	private static final String PRODUCTION_DB = "clearinghouse";
//	private static final String STAGING_DB = "stagedb";
	private static final String TESTING_DB = "testdb";
	private static final String DEVELOPMENT_DB = "devdb";

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";	
//	public static final String EVENT_REPORTS = "testCollection";	//DELME: DATA-50
//	public static final String HOSTS = "testHosts";
//	public static final String IPS = "testIPs";
//	public static final String ASNS = "testCollection";
//	public static final String SOURCES = "data_sources";

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	protected MongoDB() {
		try {
			m = new Mongo();
			db = m.getDB(DEVELOPMENT_DB);
		} catch (UnknownHostException | MongoException e) {
			LOG.error("Unable to access database:\t{}", e.getMessage());
		}
	}
	
	public static MongoDB getInstance() {
		if (instance == null) {
			instance = new MongoDB();
		}
		return instance;
	}
	
	/**
	 * Switches the database for use in unit testing
	 */
	public void switchToTestDB() {
		db = m.getDB(TESTING_DB);
	}
	
	/**
	 * Gets the database object associated with this instance
	 * @return MongoDB database object
	 */
	public DB getDB() {
		return db;
	}	
}
