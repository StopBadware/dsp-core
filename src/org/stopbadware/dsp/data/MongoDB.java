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
	private static final String TESTING_DB = "testdb";
	private static final String DEVELOPMENT_DB = "devdb";
	private static final String SECURITY_DB = "security";

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";
	
	public static final String ACCOUNTS = "accounts";
	public static final String ROLES = "roles";

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	/**
	 * Switches the database for use in unit testing
	 */
	public static void switchToTestDB() {
		db = m.getDB(TESTING_DB);
	}
	
	/**
	 * Gets the general purpose clearinghouse database object
	 * @return MongoDB database object
	 */
	public static DB getDB() {
		if (m == null) {
			try {
				m = new Mongo();
			} catch (UnknownHostException | MongoException e) {
				LOG.error("Unable to access database:\t{}", e.getMessage());
			}
		}
		
		if (m != null && db == null) {
			db = m.getDB(DEVELOPMENT_DB);	//TODO: DATA-50 change to prod
		}
		return db;
	}
	
	/**
	 * Gets the security database object
	 * @return MongoDB database object
	 */
	public static DB getSecurityDB() {
		if (m == null) {
			try {
				m = new Mongo();
			} catch (UnknownHostException | MongoException e) {
				LOG.error("Unable to access database:\t{}", e.getMessage());
			}
		}
		
		if (m != null) {
			return m.getDB(SECURITY_DB);
		} else {
			return null;
		}
	}
}
