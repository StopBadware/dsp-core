package org.stopbadware.dsp.data;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import com.mongodb.util.JSON;

public class DBHandler {
	
	private DB db;
	private DBCollection eventReportColl;
	private DBCollection hostColl;
	private DBCollection ipColl;
	private DBCollection asColl;
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	public static final String EVENT_REPORTS = MongoDB.EVENT_REPORTS;
	public static final String HOSTS = MongoDB.HOSTS;
	public static final String IPS = MongoDB.IPS;
	public static final String ASNS = MongoDB.ASNS;
	public static final String SOURCES = MongoDB.SOURCES;
	public static final String DB_DATE_PATTERN = MongoDB.DATE_PATTERN;
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;
	private static final int BATCH_SIZE = 2000;
	
	public DBHandler() {
		db = MongoDB.getInstance().getDB();
		eventReportColl = db.getCollection(EVENT_REPORTS);
		hostColl = db.getCollection(HOSTS);
		ipColl = db.getCollection(IPS);
		asColl = db.getCollection(ASNS);
	}
	
	public SearchResults testFind(long sinceTime) {
		SearchResults sr = new SearchResults();
		DBObject query = new BasicDBObject();
		//query.put("reported_date", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
		query.put("reported_date", new BasicDBObject(new BasicDBObject("$gte", new Date(sinceTime))));
		int limit = 2500;
//		DBCursor cur = eventReportColl.find(query).limit(limit);
		List<DBObject> res = eventReportColl.find(query).limit(limit).toArray();
		sr.setCount(res.size());
		sr.setSearchCriteria(String.valueOf(sinceTime));
		sr.setResults(res);
		return sr;
	}
	
	/**
	 * Inserts multiple Event Reports into database. If duplicate md5 of url, reported_date, 
	 * and reporting_source exists, will update previous entry with any changes.
	 * @param values - a set of key/value maps to be inserted
	 * @return int: number of inserts (or updates) that were successful
	 */
	public int addToEventReports(Set<Map<String, Object>> values) {
		int dbWrites = 0;
		for (Map<String, Object> m : values) {
			boolean wroteToDB = addToEventReports(m);
			if (wroteToDB) {
				dbWrites++;
			}
		}
		return dbWrites;
	}
	
	/**
	 * Inserts a single Event Report into database. If duplicate md5 of url, reported_date, 
	 * and reporting_source exists, will update previous entry with any changes.
	 * @param values - key/value map to be inserted
	 * @return boolean: true if the insert (or update) was successful
	 */
	public boolean addToEventReports(Map<String, Object> values) {
		boolean wroteToDB = false;
		DBObject doc = new BasicDBObject();
		doc.putAll(values);
		DBObject query = new BasicDBObject();
		query.put("md5", doc.get("md5"));
		query.put("reported_date", doc.get("reported_date"));
		query.put("reporting_source", doc.get("reporting_source"));
		
		WriteResult wr = eventReportColl.update(query, doc, true, false);
		if (wr.getError() != null) {
			if (!wr.getError().contains("E11000")) {
				LOG.error("Error writing {} report to collection: {}", doc.get("url"), wr.getError());
			}
		} else  {
			wroteToDB = true;
		}

		return wroteToDB;	
	}
}
