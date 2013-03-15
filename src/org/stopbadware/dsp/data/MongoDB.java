package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.Mongo;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

public abstract class MongoDB {
	
	private static DB db = null;
	private static final Logger LOG = LoggerFactory.getLogger(MongoDB.class);
	private static final String TESTING_DB = "testdb";
	private static final String DEVELOPMENT_DB = "devdb";

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";
	
	public static final String ACCOUNTS = "accounts";
	public static final String ROLES = "roles";

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	static {
		String mongoURI = System.getenv("MONGOLAB_URI");
		try {
			if (mongoURI != null) {
				MongoURI m = new MongoURI(mongoURI);
				if (m != null) {
					db = m.connectDB();
					db.authenticate(m.getUsername(), m.getPassword());
				}
			} else {
				Mongo mongo = new Mongo();
				db = mongo.getDB(DEVELOPMENT_DB);
			}
		} catch (UnknownHostException | MongoException e) {
			LOG.error("Unable to access database:\t{}", e.getMessage());
		}
	}
	
	/**
	 * Switches the database for use in unit testing
	 * @throws UnknownHostException 
	 */
	public static void switchToTestDB() throws UnknownHostException {
		Mongo mongo = new Mongo();
		db = mongo.getDB(TESTING_DB);
	}
	
	/**
	 * Gets the mongodb clearinghouse database object
	 * @return MongoDB database object
	 */
	public static DB getDB() {
		if (db == null) {
			LOG.error("***NO DATABASE CONNECTED, RETURNING NULL***");
		}
		return db;
	}
}
