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
	private static DB secdb = null;	
	
	private static String USERNAME = "dspcore";
	private static char[] PASSWORD = "6Im9fHJoaM4w".toCharArray();

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
	
	//TODO: DATA-63 add mode setting
	static {
		String host = (System.getenv("MONGO_HOST")!=null) ? System.getenv("MONGO_HOST") : "localhost";
		int port = (System.getenv("MONGO_PORT")!=null) ? Integer.valueOf(System.getenv("MONGO_PORT")) : 27017;
		String dbname = (System.getenv("MONGO_DB")!=null) ? System.getenv("MONGO_DB") : DEVELOPMENT_DB;
		String username = (System.getenv("MONGO_USER")!=null) ? System.getenv("MONGO_USER") : "";
		char[] password = (System.getenv("MONGO_PW")!=null) ? System.getenv("MONGO_PW").toCharArray() : new char[0];
		try {
//			m = new Mongo("ds055897.mongolab.com", 55897);
			m = new Mongo(host, port);
			db = m.getDB(dbname);
			if (username != null && username.length() > 0) {
				db.authenticate(username, password);
			}
		} catch (UnknownHostException | MongoException e) {
			LOG.error("Unable to access database:\t{}", e.getMessage());
		}
	}
	
	public enum Mode {
		DEV,
		TEST,
		STAGING,
		PRODUCTION;
		
		public static Mode castFromString(String str) {
			Mode mode = DEV;
			for (Mode m : Mode.values()) {
				if (str.equalsIgnoreCase(m.toString())) {
					mode = m;
					break;
				}
			}
			return mode;
		}
	}
	
	/**
	 * Switches the database for use in unit testing
	 */
	public static void switchToTestDB() {
		db = m.getDB(TESTING_DB);
		secdb = m.getDB(TESTING_DB);
	}
	
	/**
	 * Gets the general purpose clearinghouse database object
	 * @return MongoDB database object
	 */
	public static DB getDB() {
		if (m != null && db == null) {		//heroku_app12803294
			db = m.getDB("heroku_app12803294");
//			db = m.getDB(DEVELOPMENT_DB);	//TODO: DATA-50 change to prod
		}
		return db;
	}
	
	/**
	 * Gets the security database object
	 * @return MongoDB database object
	 */
	public static DB getSecurityDB() {
		if (m != null && secdb == null) {
			secdb = m.getDB(DEVELOPMENT_DB);
		}
		return secdb;
	}
}
