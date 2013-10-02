package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoException;
import com.mongodb.MongoURI;

public abstract class MongoDB {
	
	private static DB db = null;
	private static final Logger LOG = LoggerFactory.getLogger(MongoDB.class);

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";
	
	public static final String ACCOUNTS = "accounts";
	public static final String ROLES = "roles";

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	static {
		String mongoURI = System.getenv("MONGO_URL");
		try {
			if (mongoURI != null) {
				MongoURI m = new MongoURI(mongoURI);
				if (m != null) {
					db = m.connectDB();
					db.authenticate(m.getUsername(), m.getPassword());
				}
			} else {
				LOG.error("No database specified, 'MONGO_URL' must be set as an environment variable!");
			}
		} catch (UnknownHostException | MongoException e) {
			LOG.error("Unable to access database:\t{}", e.getMessage());
		}
	}
	
	/**
	 * Gets the mongodb database object specified in the MONGO_URL environment variable
	 * @return MongoDB database object
	 */
	public static DB getDB() {
		if (db == null) {
			LOG.error("***NO DATABASE CONNECTED, RETURNING NULL***");
		}
		return db;
	}
}
