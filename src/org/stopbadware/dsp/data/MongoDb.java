package org.stopbadware.dsp.data;

import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientOptions;
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
				MongoClientOptions.Builder options = new MongoClientOptions.Builder();
				options.autoConnectRetry(true);
				int maxConnections = (System.getenv("MONGO_MAX_CONN")!=null) ? Integer.valueOf(System.getenv("MONGO_MAX_CONN")) : 10;
				options.connectionsPerHost(maxConnections);
				int connectTimeout = (System.getenv("MONGO_CONN_TIMEOUT_SEC")!=null) ? Integer.valueOf(System.getenv("MONGO_CONN_TIMEOUT_SEC")) * 1000 : 30000;
				options.connectTimeout(connectTimeout);
				int socketTimeout = (System.getenv("MONGO_SOCK_TIMEOUT_SEC")!=null) ? Integer.valueOf(System.getenv("MONGO_SOCK_TIMEOUT_SEC")) * 1000 : 0;
				options.socketTimeout(socketTimeout);
				MongoClientURI mongoUri = new MongoClientURI(mongoUrl, options);
				if (mongoUri != null) {
					MongoClient mongoClient = new MongoClient(mongoUri);
					db = mongoClient.getDB(mongoUri.getDatabase());
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
			LOG.error("*** NO DATABASE CONNECTED ***");
		}
		return db;
	}
}
