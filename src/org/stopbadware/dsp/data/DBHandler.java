package org.stopbadware.dsp.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.IP;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/**
 * Class to handle all MongoDB database operations 
 */
public class DBHandler {
	
	private DB db;
	private DBCollection hostColl;
	private DBCollection ipColl;
	private DBCollection asColl;
	private DBCollection eventReportColl;
	private Subject subject; 
	private static final String DUPE_ERR = "E11000";
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;

	//TODO: DATA-72 populate prefix/fullname mapping
	public DBHandler(Subject subject) {
		db = MongoDB.getDB();
		eventReportColl = db.getCollection(MongoDB.EVENT_REPORTS);
		hostColl = db.getCollection(MongoDB.HOSTS);
		ipColl = db.getCollection(MongoDB.IPS);
		asColl = db.getCollection(MongoDB.ASNS);
		if (subject.isAuthenticated()) {
			this.subject = subject;
		}
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
	
	/**
	 * Convenience method for retrieving the field to use as the reported by
	 * @param reporter reporting source for the record
	 * @return "_reported_by" if the length of reporter is greater than 5 chars, 
	 * "prefix" otherwise
	 */
	private String getReporterField(String reporter) {
		//TODO: DATA-72 refactor logic to check if <=5 return prefix else find prefix 
		return (reporter.length() > 5) ? "reported_by" : "prefix";
	}
	
	/**
	 * Convenience method to check for authorization and log a
	 * warning for unauthorized access attempts
	 * @param permission the permission to check if the principal is
	 * authorized for
	 * @return true if the principal has the appropriate permission
	 */
	private boolean isAuthorized(String permission) {
		boolean auth = subject.isPermitted(permission);
		if (!auth) {
			LOG.warn("{} NOT authorized for {}", subject.getPrincipal(), permission);
		}
		return auth;
	}
	
	/**
	 * Finds Event Reports since the specified timestamp, up to a maximum of 25K
	 * sorted by reported at time ascending
	 * @param sinceTime UNIX timestamp to retrieve reports since
	 * @return SearchResults with the results or null if not authorized
	 */
	public SearchResults findEventReportsSince(long sinceTime) {
		if (sinceTime < 0 || !isAuthorized(Permissions.READ_EVENTS)) {
			return null;
		}
		SearchResults sr = new SearchResults(String.valueOf(sinceTime));
		DBObject query = new BasicDBObject("reported_at", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
		DBObject keys = new BasicDBObject("_id", 0);
		keys.put("_created", 0);
		DBObject sort = new BasicDBObject("reported_at", ASC);
		int limit = 25000;
		List<DBObject> res = eventReportColl.find(query, keys).sort(sort).limit(limit).toArray();
		sr.setCount(res.size());
		sr.setResults(res);
		return sr;
	}
	
	public SearchResults getEventReportsStats(String source) {
		if (!isAuthorized(Permissions.READ_EVENTS)) {
			return null;
		}
		SearchResults sr = new SearchResults(source);
		Map<String, Object> stats = new HashMap<>();
		long start = System.currentTimeMillis();		//DELME
		stats.put("total_count", eventReportColl.getCount());
		long er = System.currentTimeMillis()-start;		//DELME
		stats.put("on_blacklist_count", eventReportColl.getCount(new BasicDBObject("is_on_blacklist", true)));
		long bl = System.currentTimeMillis()-start;		//DELME
		stats.put("unique_host_count", eventReportColl.distinct("host").size());
		long host = System.currentTimeMillis()-start;	//DELME
		System.out.println(start);	//DELME
		System.out.println(er);		//DELME
		System.out.println(bl);		//DELME
		System.out.println(host);	//DELME
		sr.setCount(stats.size());
		sr.setResults(stats);
		
		return sr;
	}
	
	/**
	 * Retrieves and returns timestamp of last event report for the requested source
	 * @param source either full name or prefix of reporting source (case insensitive)
	 * @return TimeOfLast with UNIX timestamp (0 if unable to determine)
	 */
	public TimeOfLast getTimeOfLast(String source) {
		long time = 0L;
		DBObject query = new BasicDBObject();
		Pattern sourceRegex = getRegex(source);
		String sourceField = getReporterField(source);
		query.put(sourceField, sourceRegex);
		
		DBObject keys = new BasicDBObject();
		keys.put("_id", 0);
		keys.put("reported_at", 1);
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_EVENTS)) {
			cur = eventReportColl.find(query, keys).sort(new BasicDBObject ("reported_at", DESC)).limit(1);
		}
		while (cur != null && cur.hasNext()) {
			try {
				time = Long.valueOf(cur.next().get("reported_at").toString());
			} catch (NumberFormatException | NullPointerException e) {
				time = 0L;
			}
		}
		return new TimeOfLast(source, time);
	}
	
	/**
	 * Finds and returns all hosts of currently blacklisted event reports
	 * @return Set of Strings containing the blacklisted hosts
	 */
	public Set<String> getCurrentlyBlacklistedHosts() {
		Set<String> hosts = new HashSet<>();
		DBObject query = new BasicDBObject();
		query.put("is_on_blacklist", true);
		DBObject keys = new BasicDBObject();
		keys.put("_id", 0);
		keys.put("host", 1);
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_EVENTS)) {
			cur = eventReportColl.find(query, keys);
		}
		while (cur != null && cur.hasNext()) {
			try {
				hosts.add(cur.next().get("host").toString());
			} catch (MongoException | NullPointerException e) {
				LOG.error("Unable to retrieve object from cursor:\t{}", e.getMessage());
			}
		}
		return hosts;
	}
	
	/**
	 * Inserts multiple Event Reports into database, ignoring duplicates.
	 * @param reports a set of key/value maps to be inserted 
	 * @return int: number of new documents written to database plus 
	 * duplicate documents that were ignored 
	 */
	public int addEventReports(Set<ERWrapper> reports) {	
		int dbWrites = 0;
		int dbDupes = 0;
		for (ERWrapper er : reports) {
			WriteResult wr = null;
			if (er.getErMap() != null && er.getErMap().size() > 0) {
				wr = addEventReport(er.getErMap());
			}
			if (wr != null) {
				if (wr.getError() != null && wr.getError().contains(DUPE_ERR)) {
					dbDupes++;
				} else if (wr.getError() == null) {
					dbWrites++; 
				}
			}
			addHost(er.getHost(), ShareLevel.castFromString(er.getShareLevel()));
		}
		
		LOG.info("{} new event reports added", dbWrites);
		LOG.info("{} duplicate entries ignored", dbDupes);
		return dbWrites+dbDupes;
	}
	
	/**
	 * Inserts a single Event Report into database, ignoring duplicates.
	 * @param event key/value map to be inserted
	 * @return WriteResult: the WriteResult associated with the write attempt
	 * or null if the attempt was unsuccessful
	 */
	private WriteResult addEventReport(Map<String, Object> event) {
		DBObject doc = new BasicDBObject();
		doc.putAll(event);
		doc.put("_created", System.currentTimeMillis() / 1000);
		
		WriteResult wr = null;
		try {
			if (isAuthorized(Permissions.WRITE_EVENTS)) {
				wr = eventReportColl.insert(doc);
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					if (doc.get("url") != null) {
						LOG.error("Error writing {} report to collection: {}", doc.get("url"), wr.getError());
					} else {
						LOG.error("Error writing report with null URL to collection: {}", wr.getError());
					}
				}
			}
		} catch (MongoException e) {
			LOG.error("MongoException thrown when adding event report:\t{}", e.getMessage());
		}
		
		return wr;	
	}
	
	/**
	 * Adds a single host to the hosts collection
	 * @param host the host to add
	 * @param level the ShareLevel it was reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return boolean: true if the insert (or update) was successful
	 */
	private boolean addHost(String host, ShareLevel level) {
		if (host == null || host.length() < 1) {
			return false;
		}
		boolean wroteToDB = false;
		DBObject query = new BasicDBObject();
		query.put("host", host);
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_HOSTS)) {
			cur = hostColl.find(query);
		}
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

		WriteResult wr = null;
		if (isAuthorized(Permissions.WRITE_HOSTS)) {
			wr = hostColl.update(query, updateDoc, true, false);
		}
		if (wr != null && wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
			LOG.error("Error writing {} to host collection: {}", host, wr.getError());
		} else {
			wroteToDB = true;
		}
		return wroteToDB;	
	}
	
	/**
	 * Writes the IP address associated with each host in the map provided 
	 * @param ips a Map containing keys of hosts as Strings and values of IP addresses as longs
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
			ipDoc.put("timestamp", System.currentTimeMillis()/1000);
			
			DBObject updateDoc = new BasicDBObject();
			updateDoc.put("$push", new BasicDBObject("ips", ipDoc));
			if (ipHasChanged(host, ip)) {
				if (isAuthorized(Permissions.WRITE_HOSTS)) {
					WriteResult wr = hostColl.update(hostDoc, updateDoc);
					if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
						LOG.error("Error writing to collection: {}", wr.getError());
					} else  {
						dbWrites += wr.getN();
					}
				}
			}
		}
		LOG.info("Wrote associated IP addresses for {} hosts", dbWrites);
		return dbWrites;		
	}
	
	/**
	 * Checks if the most recent IP entry for the host differs from a potentially new entry
	 * @param host the host to check
	 * @param ip the new IP address
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean ipHasChanged(String host, long ip) {
		long mostRecentTimestamp = 0L;
		long mostRecentIP = -1;
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_HOSTS)) {
			cur = hostColl.find(new BasicDBObject("host", host), new BasicDBObject("ips", 1));
		}
		
		while (cur != null && cur.hasNext()) {
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
							/*Set to -1 to force write of new entry if unable to cast db entry*/
							mostRecentIP = -1; 
						}
					}
				}
			}
		}
		
		return (mostRecentIP != ip);
	}
	
	/**
	 * Adds an IP address to the database
	 * @param ip the IP address to add
	 */
	private boolean addIP(long ip) {
		boolean wroteToDB = false;
		DBObject ipDoc = new BasicDBObject();
		ipDoc.put("ip", ip);
		if (isAuthorized(Permissions.WRITE_IPS)) {
			WriteResult wr = ipColl.insert(ipDoc);
			if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
				LOG.error("Error writing {} / {} to database", ip, IP.longToDots(ip));
			} else {
				wroteToDB = true;
			}
		}
		return wroteToDB;
	}
	
	/**
	 * Writes the ASNs associated with each IP in the map provided 
	 * @param asns a Map containing keys of IP addresses as longs and AS objects as values
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
			asnDoc.put("asn", asns.get(ip).getAsn());
			asnDoc.put("timestamp", System.currentTimeMillis()/1000L);
			
			DBObject updateDoc = new BasicDBObject();
			updateDoc.put("$push", new BasicDBObject("asns", asnDoc));
			if (asnHasChanged(ip, asns.get(ip).getAsn())) {
				if (isAuthorized(Permissions.WRITE_IPS)) {
					WriteResult wr = ipColl.update(ipDoc, updateDoc);
					if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
						LOG.error("Error writing to collection: {}", wr.getError());
					} else  {
						dbWrites += wr.getN();
					}
				}
			}
			
		}
		addAutonmousSystem(as);
		LOG.info("Associated {} Autonomous Systems with IP addresses", dbWrites);
		return dbWrites;		
	}
	
	/**
	 * Checks if the most recent AS entry for the IP differs from a potentially new entry
	 * @param ip the IP to check
	 * @param asn the new ASN
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean asnHasChanged(long ip, int asn) { 
		boolean hasChanged = false;
		long mostRecentTimestamp = 0L;
		int mostRecentASN = asn;
		
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_IPS)) {
			cur = ipColl.find(new BasicDBObject("ip", ip), new BasicDBObject("asns", 1));
		}
		
		while (cur != null && cur.hasNext()) {
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
	 * @param autonomousSystems the set containing the Autonomous Systems to add
	 * @return int: the number of inserted or updated documents
	 */
	private int addAutonmousSystem(Set<AutonomousSystem> autonomousSystems) {
		int dbWrites = 0;
		for (AutonomousSystem as : autonomousSystems) {
			int asn = as.getAsn();
			if (asn > 0) {
				DBObject doc = new BasicDBObject();
				doc.put("asn", asn);
				doc.put("country", as.getCountry());
				doc.put("name", as.getName());
				
				DBObject asnDoc = new BasicDBObject();
				asnDoc.put("asn", asn);
				
				if (isAuthorized(Permissions.WRITE_ASNS)) {
					WriteResult wr = asColl.update(asnDoc, doc, true, false);
					if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
						LOG.error("Error writing ASN {} to collection: {}", asn, wr.getError());
					} else {
						dbWrites += wr.getN();
					}
				}
			}
		}
		LOG.info("Wrote {} Autonomous Systems to database", dbWrites);
		return dbWrites;
	}
	
	/**
	 * Find the event reports currently marked as blacklisted for the reporter provided
	 * @param reporter blacklisting source
	 * @param field the key to retrieve from each report
	 * @return a Set of Strings containing the value for the corresponding key
	 */
	private Set<String> findCurrentlyBlacklistedBySource(String reporter, String field) {
		DBObject query = new BasicDBObject();
		String sourceField = getReporterField(reporter);
		query.put(sourceField, getRegex(reporter));
		query.put("is_on_blacklist", true);
		DBObject keys = new BasicDBObject(field, 1);
		keys.put("_id", 0);
		DBCursor cur = null;
		if (isAuthorized(Permissions.READ_EVENTS)) {
			cur = eventReportColl.find(query, keys);
		}
		Set<String> blacklisted = new HashSet<>();

		while (cur != null && cur.hasNext()) {
			Object val = cur.next().get(field);
			if (val != null) {
				blacklisted.add(val.toString());
			}
		}
		
		return blacklisted;
	}
	
	/**
	 * Updates event reports for the specified reporter matching the provided key/value 
	 * to have an is_on_blacklist flag of false and sets the removed_from_blacklist time
	 * @param reporter either the full name or prefix of the reporting entity
	 * @param key db document field to match
	 * @param value matching value
	 * @param removedTime UNIXTimestamp as a long to set as the removed time
	 * @return int: the number of event reports updated
	 */
	private int removeFromBlacklist(String reporter, String key, Object value, long removedTime) {
		int updated = 0;
		String sourceField = getReporterField(reporter);
		DBObject query = new BasicDBObject();
		query.put(key, value);
		query.put(sourceField, reporter);
		query.put("is_on_blacklist", true);
		
		DBObject update = new BasicDBObject();
		update.put("is_on_blacklist", false);
		update.put("removed_from_blacklist", removedTime);
		WriteResult wr = null;
		if (isAuthorized(Permissions.WRITE_EVENTS)) {
			wr = eventReportColl.update(query, new BasicDBObject("$set", update), false, true);
		}
		if (wr != null) {
			if (wr.getError() != null) {
				LOG.error("Error changing blacklist flag for {}:\t{}", value, wr.getError());
			} else if (wr.getError() == null) {
				updated += wr.getN();
			}
		}
		
		return updated;
	}
	
	/**
	 * Updates event reports for the specified reporter matching hosts in the provided set
	 * to have an is_on_blacklist flag of false and sets the removed_from_blacklist time 
	 * @param reporter either the full name or prefix of the reporting entity
	 * @param removedTime UNIXTimestamp as a long to set as the removed time
	 * @param cleanHosts the set of hosts to match
	 * @return int: the number of event reports updated
	 */
	public int updateBlacklistFlagsFromCleanHosts(String reporter, long removedTime, Set<String> cleanHosts) {
		int updated = 0;
		String key = "host";
		Set<String> blacklisted = findCurrentlyBlacklistedBySource(reporter, key);
		for (String blHost : blacklisted) {
			if (cleanHosts.contains(blHost)) {
				updated += removeFromBlacklist(reporter, key, blHost, removedTime);
			}
		}
		
		return updated;
	}
	
	/**
	 * Updates event reports for the specified reporter that do NOT match reports 
	 * in the provided set to have an is_on_blacklist flag of false and 
	 * sets the removed_from_blacklist time 
	 * @param reporter either the full name or prefix of the reporting entity
	 * @param removedTime UNIXTimestamp as a long to set as the removed time
	 * @param set the set of still blacklisted reports
	 * @return int: the number of event reports updated
	 */
	public int updateBlacklistFlagsFromDirtyReports(String reporter, long removedTime, Set<ERWrapper> set) {
		String key = "sha2_256";
		Set<String> sha2urls = new HashSet<>(set.size());
		for (ERWrapper er : set) {
			Map<String, Object> map = er.getErMap();
			if (map != null) {
				if (map.containsKey(key)) {
					sha2urls.add(map.get(key).toString());
				} else if (map.containsKey(key)) {
					sha2urls.add(SHA2.get256(map.get("url").toString()));
				}
			}
		}
		
		return updateBlacklistFlagsFromDirty(reporter, removedTime, sha2urls, key);
	}
	
	/**
	 * Updates event reports for the specified reporter that do NOT match reports 
	 * in the provided set based on the key to have an is_on_blacklist flag of false and 
	 * sets the removed_from_blacklist time  
	 * @param reporter either the full name or prefix of the reporting entity
	 * @param removedTime UNIXTimestamp as a long to set as the removed time
	 * @param dirtyValues the set of reports to remain blacklisted
	 * @param key the field the dirtyValues correspond to
	 * @return int: the number of event reports updated
	 */
	private int updateBlacklistFlagsFromDirty(String reporter, long removedTime, Set<String> dirtyValues, String key) {
		int updated = 0;
		Set<String> blacklisted = findCurrentlyBlacklistedBySource(reporter, key);

		for (String blValue : blacklisted) {
			if (!dirtyValues.contains(blValue)) {
				updated += removeFromBlacklist(reporter, key, blValue, removedTime);
			}
		}
		
		return updated;
	}
}
