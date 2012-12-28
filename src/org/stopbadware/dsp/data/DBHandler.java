package org.stopbadware.dsp.data;

import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.AutonomousSystem;
import org.stopbadware.dsp.ShareLevel;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class DBHandler {
	
	private DB db;
	private DBCollection eventReportColl;
	private DBCollection hostColl;
	private DBCollection ipColl;
	private DBCollection asColl;
	private static final String DUPE_ERR = "E11000";
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
		for (Map<String, Object> map : values) {
			boolean wroteToDB = addToEventReports(map);
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
			if (!wr.getError().contains(DUPE_ERR)) {	/*Ignore duplicate entry errors*/
				LOG.error("Error writing {} report to collection: {}", doc.get("url"), wr.getError());
			}
		} else  {
			wroteToDB = true;
		}

		return wroteToDB;	
	}
	
	/**
	 * Adds multiple hosts to the hosts collection
	 * @param hosts - a set of hosts to add
	 * @param level - the ShareLevel the hosts were reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return int: number of inserts (or updates) that were successful
	 */
	public int addToHosts(Set<String> hosts, ShareLevel level) {
		int dbWrites = 0;
		for (String host : hosts) {
			boolean wroteToDB = addToHosts(host, level);
			if (wroteToDB) {
				dbWrites++;
			}
		}
		return dbWrites; 
	}
	
	/**
	 * Adds a single host to the hosts collection
	 * @param host - the host to add
	 * @param level - the ShareLevel it was reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return boolean: true if the insert (or update) was successful
	 */
	public boolean addToHosts(String host, ShareLevel level) {
		if (host == null || host.length() < 1) {
			return false;
		}
		boolean wroteToDB = false;
		DBObject query = new BasicDBObject();
		query.put("host", host);
		DBCursor cur = hostColl.find(query);
		while (cur.hasNext()) {
			String levelString = (String) cur.next().get("share_level");
			if (levelString != null) {
				ShareLevel curLevel = ShareLevel.castFromString(levelString);
				level = ShareLevel.getLeastRestrictive(curLevel, level);
			}
		}
		
		DBObject updateDoc = new BasicDBObject();
		updateDoc.put("host", host);
		updateDoc.put("share_level", level.toString());

		WriteResult wr = hostColl.update(query, updateDoc, true, false);
		if (wr.getError() != null) {
			if (!wr.getError().contains(DUPE_ERR)) {	/*Ignore duplicate entry errors*/
				LOG.error("Error writing {} to collection: {}", host, wr.getError());
			}
		} else {
			wroteToDB = true;
		}
		return wroteToDB;	
	}
	
	//add ips
	
	/**
	 * Writes the ASNs associated with each IP in the map provided 
	 * @param asns - a Map containing keys of IP addresses as longs and AS objects as values
	 * @return int: the number of inserted or updated documents
	 */
	public int addASNsForIPs(Map<Long, AutonomousSystem> asns) {
		int upsertedDocs = 0;
		Set<AutonomousSystem> as = new HashSet<>(asns.size());
		for (long ip : asns.keySet()) {
			as.add(asns.get(ip));
			
			DBObject ipDoc = new BasicDBObject();
			ipDoc.put("ip", ip);
			
			DBObject asnDoc = new BasicDBObject();
			asnDoc.put("asn", asns.get(ip).getASN());
			asnDoc.put("timestamp", System.currentTimeMillis()/1000L);
			
			DBObject updateDoc = new BasicDBObject();
			updateDoc.put("$push", new BasicDBObject("asns", asnDoc));
			if (asnHasChanged(ip, asns.get(ip).getASN())) {
				WriteResult wr = ipColl.update(ipDoc, updateDoc);
				if (wr.getError() != null) {
					LOG.error("Error writing to collection: {}",wr.getError());
				} else  {
					upsertedDocs += wr.getN();
				}
			}
			
		}
		//addASsToDB(as);	//TODO: add subroutine
		return upsertedDocs;		
	}
	
	/**
	 * Checks if the most recent AS entry for the IP differs from a potentially new entry
	 * @param ip - the IP to check
	 * @param asn - the new ASN
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean asnHasChanged(long ip, int asn) { 
		boolean hasChanged = false;
		DBCursor cur = ipColl.find(new BasicDBObject("ip", ip), new BasicDBObject("asns", 1));
		long mostRecentTimestamp = 0L;
		int mostRecentASN = asn;
		
		while (cur.hasNext()) {
			BasicDBList asns = (BasicDBList) ((BasicDBObject) cur.next()).get("asns");
			if (asns == null || asns.size() == 0) {
				hasChanged = true; /*set to true if no ASN entries*/
			} else {
				for (String as : asns.keySet()) {
					long timestamp = 0L;
					try {
						timestamp =  (long) ((BasicDBObject) asns.get(as)).get("timestamp");
					} catch (ClassCastException e) {
						continue; /*Skip if cannot cast time from db*/
					}						
					if (timestamp > mostRecentTimestamp) {
						mostRecentTimestamp = timestamp;
						try {
							mostRecentASN = (int) ((BasicDBObject) asns.get(as)).get("asn");
						} catch (ClassCastException e) {
							/*Set to 0 to force write of new entry if unable to cast db entry*/
							mostRecentASN = 0;
						}
					}
				}
			}
		}
		
		if (mostRecentASN != asn) {
			hasChanged = true;
		}
		
		return hasChanged;
	}
}
