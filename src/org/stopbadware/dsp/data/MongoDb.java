package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoException;

public abstract class MongoDb {
	
	private static DB db = null;
	private static final Logger LOG = LoggerFactory.getLogger(MongoDb.class);

	public static final String EVENT_REPORTS = "event_reports";
	public static final String HOSTS = "hosts";
	public static final String IPS = "ips";
	public static final String ASNS = "autonomous_systems";
	
	public static final String ACCOUNTS = "accounts";
	public static final String ROLES = "roles";

	public static final int ASC = 1;
	public static final int DESC = -1;
	
	static {
		String mongoUrl = System.getenv("MONGO_URL");
		try {
			if (mongoUrl != null) {
				MongoClientURI mongoUri = new MongoClientURI(mongoUrl);
				MongoClient mongoClient = new MongoClient(mongoUri);
				if (mongoUri != null) {
					db = mongoClient.getDB(mongoUri.getDatabase());
					if (mongoUri.getUsername() != null && mongoUri.getPassword() != null) {
						db.authenticate(mongoUri.getUsername(), mongoUri.getPassword());
					}
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
	 * @return a MongoDB database object, or null if no database provided
	 */
	public static DB getDB() {
		if (db == null) {
			LOG.error("***NO DATABASE CONNECTED, RETURNING NULL***");
		}
		return db;
	}
}
