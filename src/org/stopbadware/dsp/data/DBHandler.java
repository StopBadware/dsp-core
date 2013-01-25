package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.AutonomousSystem;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.lib.util.IP;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class DBHandler {
	
	private DB db;
	private DBCollection eventReportColl;
	private DBCollection hostColl;
	private DBCollection ipColl;
	private DBCollection asColl;
	private static final String DUPE_ERR = "E11000";
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;
	
	public DBHandler() {
		db = MongoDB.getInstance().getDB();
		eventReportColl = db.getCollection(MongoDB.EVENT_REPORTS);
		hostColl = db.getCollection(MongoDB.HOSTS);
		ipColl = db.getCollection(MongoDB.IPS);
		asColl = db.getCollection(MongoDB.ASNS);
	}
	
	/**
	 * Convenience method for creating an exact case insensitive regex Pattern
	 * @param str String to match
	 * @return java.util.regex Pattern or null if could not create Pattern
	 */
	private Pattern getRegex(String str) {
		Pattern p = null;
		try {
			p = Pattern.compile("^" + str + "$", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
		} catch (IllegalArgumentException e) {
			LOG.warn("Unable to create Pattern for >>{}<<\t{}", str, e.getMessage());
		}
		return p;
	}
	
	public SearchResults testFind(long sinceTime) {
		SearchResults sr = new SearchResults();
		DBObject query = new BasicDBObject();
		query.put("reported_at", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
		int limit = 2500;
//		DBCursor cur = eventReportColl.find(query).limit(limit);
		List<DBObject> res = eventReportColl.find(query).limit(limit).toArray();
		sr.setCount(res.size());
		sr.setSearchCriteria(String.valueOf(sinceTime));
		sr.setResults(res);
		return sr;
	}
	
	/**
	 * Retrieves and returns timestamp of last event report for the requested source
	 * @param source - either full name or prefix of reporting source (case insensitive)
	 * @return TimeOfLast with UNIX timestamp (0 if unable to  
	 */
	public TimeOfLast getTimeOfLast(String source) {
		long time = 0L;
		DBObject query = new BasicDBObject();
		Pattern sourceRegex = getRegex(source);
		if (source.length() <= 5) {
			query.put("prefix", sourceRegex);
		} else {
			query.put("reported_by", sourceRegex);
		}
		DBObject keys = new BasicDBObject();
		keys.put("_id", 0);
		keys.put("reported_at", 1);
		DBCursor cur = eventReportColl.find(query, keys).sort(new BasicDBObject ("reported_at", DESC)).limit(1);
		while (cur.hasNext()) {
			try {
				time = Long.valueOf(cur.next().get("reported_at").toString());
			} catch (NumberFormatException | NullPointerException e) {
				time = 0L;
			}
		}
		System.out.println(time);
		return new TimeOfLast(source, time);
	}
	
	/**
	 * Inserts multiple Event Reports into database, ignoring duplicates.
	 * @param events - a set of key/value maps to be inserted 
	 * @return int: number of new documents written to database plus 
	 * duplicate documents that were ignored 
	 */
	public int addEventReports(Set<Map<String, Object>> events) {	
		int dbWrites = 0;
		int dbDupes = 0;
		for (Map<String, Object> map : events) {
			WriteResult wr = addEventReport(map);
			if (wr != null) {
				if (wr.getError() != null && wr.getError().contains(DUPE_ERR)) {
					dbDupes++;
				} else if (wr.getError() == null) {
					dbWrites++; 
				}
			}
		}
		
		LOG.info("{} new event reports added", dbWrites);
		LOG.info("{} duplicate entries ignored", dbDupes);
		return dbWrites+dbDupes;
	}
	
	/**
	 * Inserts a single Event Report into database, ignoring duplicates.
	 * @param event - key/value map to be inserted
	 * @return WriteResult: the WriteResult associated with the write attempt
	 * or null if the attempt was unsuccessful
	 */
	private WriteResult addEventReport(Map<String, Object> event) {
		DBObject doc = new BasicDBObject();
		doc.putAll(event);
		doc.put("_created", System.currentTimeMillis() / 1000);
		
		WriteResult wr = null;
		try {
			wr = eventReportColl.insert(doc);
			if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
				if (doc.get("url") != null) {
					LOG.error("Error writing {} report to collection: {}", doc.get("url"), wr.getError());
				} else {
					LOG.error("Error writing report with null URL to collection: {}", wr.getError());
				}
			} 
		} catch (MongoException e) {
			LOG.error("MongoException thrown when adding event report:\t{}", e.getMessage());
		}
		
		return wr;	
	}
	
	/**
	 * Adds multiple hosts to the hosts collection
	 * @param hosts - a set of hosts to add
	 * @param level - the ShareLevel the hosts were reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return int: number of inserts (or updates) that were successful
	 */
	public int addHosts(Set<String> hosts, ShareLevel level) {
		int dbWrites = 0;
		for (String host : hosts) {
			boolean wroteToDB = addHost(host, level);
			if (wroteToDB) {
				dbWrites++;
			}
		}
		LOG.info("Wrote {} new hosts to database", dbWrites);
		return dbWrites; 
	}
	
	/**
	 * Adds a single host to the hosts collection
	 * @param host - the host to add
	 * @param level - the ShareLevel it was reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return boolean: true if the insert (or update) was successful
	 */
	public boolean addHost(String host, ShareLevel level) {
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
		if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
			LOG.error("Error writing {} to collection: {}", host, wr.getError());
		} else {
			wroteToDB = true;
		}
		return wroteToDB;	
	}
	
	/**
	 * Writes the IP address associated with each host in the map provided 
	 * @param ips - a Map containing keys of hosts as Strings and values of IP addresses as longs
	 * @return int: the number of inserted or updated documents
	 */
	public int addIPsForHosts(Map<String, Long> ips) {
		int dbWrites = 0;
		for (String host : ips.keySet()) {
			DBObject hostDoc = new BasicDBObject();
			hostDoc.put("host", host);
			
			long ip = ips.get(host);
			if (ip > 0) {
				addIP(ip);
			}
			DBObject ipDoc = new BasicDBObject();
			ipDoc.put("ip", ip);
			ipDoc.put("timestamp", System.currentTimeMillis()/1000L);
			
			DBObject updateDoc = new BasicDBObject();
			updateDoc.put("$push", new BasicDBObject("ips", ipDoc));
			if (ipHasChanged(host, ip)) {
				WriteResult wr = hostColl.update(hostDoc, updateDoc);
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					LOG.error("Error writing to collection: {}", wr.getError());
				} else  {
					dbWrites += wr.getN();
				}
			}
		}
		LOG.info("Associated {} hosts with IP addresses", dbWrites);
		return dbWrites;		
	}
	
	/**
	 * Checks if the most recent IP entry for the host differs from a potentially new entry
	 * @param host - the host to check
	 * @param ip - the new IP address
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean ipHasChanged(String host, long ip) {
		boolean hasChanged = false;
		DBCursor cur = hostColl.find(new BasicDBObject("host", host), new BasicDBObject("ips", 1));
		long mostRecentTimestamp = 0L;
		long mostRecentIP = ip;
		
		while (cur.hasNext()) {
			BasicDBList ips = (BasicDBList) ((BasicDBObject) cur.next()).get("ips");
			if (ips != null) {
				for (String i : ips.keySet()) {
					long timestamp = 0L;
					try {
						timestamp = (long) ((BasicDBObject) ips.get(i)).get("timestamp");
					} catch (ClassCastException e) {
						/*Skip if cannot cast time from db*/
						continue; 
					}
					if (timestamp > mostRecentTimestamp) {
						mostRecentTimestamp = timestamp;
						try {
							mostRecentIP = (long) ((BasicDBObject) ips.get(i)).get("ip");
						} catch (ClassCastException e) {
							/*Set to 0 to force write of new entry if unable to cast db entry*/
							mostRecentIP = 0L; 
						}
					}
				}
			}
		}
		
		if (mostRecentIP != ip) {
			hasChanged = true;
		}
		
		return hasChanged;
	}
	
	/**
	 * Adds an IP address to the database
	 * @param ip - the IP address to add
	 */
	private boolean addIP(long ip) {
		boolean wroteToDB = false;
		DBObject ipDoc = new BasicDBObject();
		ipDoc.put("ip", ip);
		WriteResult wr = ipColl.insert(ipDoc);
		if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
			LOG.error("Error writing {} / {} to database", ip, IP.longToDots(ip));
		} else {
			wroteToDB = true;
		}
		return wroteToDB;
	}
	
	/**
	 * Writes the ASNs associated with each IP in the map provided 
	 * @param asns - a Map containing keys of IP addresses as longs and AS objects as values
	 * @return int: the number of inserted or updated documents
	 */
	public int addASNsForIPs(Map<Long, AutonomousSystem> asns) {
		int dbWrites = 0;
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
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					LOG.error("Error writing to collection: {}", wr.getError());
				} else  {
					dbWrites += wr.getN();
				}
			}
			
		}
		addAutonmousSystem(as);
		LOG.info("Associated {} Autonomous Systems with IP addresses", dbWrites);
		return dbWrites;		
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
				/*set to true if no ASN entries*/
				hasChanged = true; 
			} else {
				for (String as : asns.keySet()) {
					long timestamp = 0L;
					try {
						timestamp =  (long) ((BasicDBObject) asns.get(as)).get("timestamp");
					} catch (ClassCastException e) {
						/*Skip if cannot cast time from db*/
						continue; 
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
	
	/**
	 * Adds Autonomous Systems to database
	 * @param autonomousSystems - the set containing the Autonomous Systems to add
	 * @return int: the number of inserted or updated documents
	 */
	private int addAutonmousSystem(Set<AutonomousSystem> autonomousSystems) {
		int dbWrites = 0;
		for (AutonomousSystem as : autonomousSystems) {
			int asn = as.getASN();
			if (asn > 0) {
				DBObject doc = new BasicDBObject();
				doc.put("asn", asn);
				doc.put("country", as.getCountry());
				doc.put("name", as.getName());
				
				DBObject asnDoc = new BasicDBObject();
				asnDoc.put("asn", asn);
				
				WriteResult wr = asColl.update(asnDoc, doc, true, false);
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					LOG.error("Error writing ASN {} to collection: {}", asn, wr.getError());
				} else {
					dbWrites += wr.getN();
				}
			}
		}
		LOG.info("Wrote {} Autonomous Systems to database", dbWrites);
		return dbWrites;
	}
	
	public int removeHostsFromBlacklist(String reporter, Set<String> cleanHosts) {
		//TODO: DATA-50 implement clean host handling
		return 0;
	}
}
