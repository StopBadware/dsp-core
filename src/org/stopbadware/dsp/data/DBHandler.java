package org.stopbadware.dsp.data;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
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
		DBCursor cur = eventReportColl.find(query).limit(1000);
		sr.setCount(cur.count());
		sr.setSearch_criteria("\"since\":"+sinceTime);
		sr.setResults(JSON.serialize(cur));
		return sr;
	}
}
