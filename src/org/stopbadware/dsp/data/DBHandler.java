package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.IP;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * Class to handle all MongoDB database operations 
 */
public class DBHandler {
	
	private DB db;
	private DBCollection ipColl;
	private DBCollection asColl;
	private EventReportsHandler eventsHandler;
	private HostsHandler hostsHandler;
	private Subject subject; 
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	public static final String DUPE_ERR = "E11000";	//DELME?
	
	public DBHandler(Subject subject) {
		db = MongoDB.getDB();
		ipColl = db.getCollection(MongoDB.IPS);
		asColl = db.getCollection(MongoDB.ASNS);
		if (subject.isAuthenticated()) {
			this.subject = subject;
			eventsHandler = new EventReportsHandler(db, this.subject);
			hostsHandler = new HostsHandler(db, this.subject);
		}
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
	}	//DELME?
	
	/**
	 * Finds Event Reports since the specified timestamp, up to a maximum of 25K
	 * sorted by reported at time ascending
	 * @param sinceTime UNIX timestamp to retrieve reports since
	 * @return SearchResults with the results or null if not authorized
	 */
	public SearchResults findEventReportsSince(long sinceTime) {
		return eventsHandler.findEventReportsSince(sinceTime);
	}
	
	/**
	 * High level statistics of the Event Reports collection
	 * @param source the source to retrieve statistics for, or 'all'
	 * @return SearchResults containing the collection statistics
	 */
	public SearchResults getEventReportsStats(String source) {
		return eventsHandler.getEventReportsStats(source);
	}
	
	/**
	 * Searches for Event Reports and returns those matching the provided criteria
	 * @param criteria Map of search criteria
	 * @return SearchResults containing the matching Event Reports
	 * @throws SearchException
	 */
	public SearchResults eventReportSearch(MultivaluedMap<String, String> criteria) throws SearchException {
		return eventsHandler.eventReportSearch(criteria);
	}
	
	/**
	 * Finds a specific Event Report based on the UID
	 * @param UID (SHA2-PREFIX-UNIXTIME) of the Event Report to find
	 * @return SearchResults containing the Event Report
	 * @throws SearchException
	 */
	public SearchResults findEventReport(String uid) throws SearchException {
		return eventsHandler.getEventReport(uid);
	}
	
	/**
	 * Retrieves and returns timestamp of last event report for the requested source
	 * @param source either full name or prefix of reporting source (case insensitive)
	 * @return TimeOfLast with UNIX timestamp (0 if unable to determine)
	 */
	public TimeOfLast getTimeOfLast(String source) {
		return eventsHandler.getTimeOfLast(source);
	}
	
	/**
	 * Finds and returns all hosts of currently blacklisted event reports
	 * @return Set of Strings containing the blacklisted hosts
	 */
	public Set<String> getCurrentlyBlacklistedHosts() {
		return eventsHandler.getCurrentlyBlacklistedHosts();
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
				wr = eventsHandler.addEventReport(er.getErMap());
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
	 * Adds a single host to the hosts collection
	 * @param host the host to add
	 * @param level the ShareLevel it was reported at (for existing entries the least restrictive ShareLevel will be used
	 * @return boolean: true if the insert (or update) was successful
	 */
	private boolean addHost(String host, ShareLevel level) {
		return hostsHandler.addHost(host, level);	
	}
	
	/**
	 * Writes the IP address associated with each host in the map provided 
	 * @param ips a Map containing keys of hosts as Strings and values of IP addresses as longs
	 * @return int: the number of inserted or updated documents
	 */
	public int addIPsForHosts(Map<String, Long> ips) {
		int dbWrites = 0;
		for (String host : ips.keySet()) {
			long ip = ips.get(host);
			if (ip > 0) {
				addIP(ip);
			}
			boolean addedOrUpdatedIP = hostsHandler.addIPForHost(host, ip);
			if (addedOrUpdatedIP) {
				dbWrites++;
			}
		}
		LOG.info("Wrote associated IP addresses for {} hosts", dbWrites);
		return dbWrites;		
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
		Set<String> blacklisted = eventsHandler.findCurrentlyBlacklistedBySource(reporter, key);
		for (String blHost : blacklisted) {
			if (cleanHosts.contains(blHost)) {
				updated += eventsHandler.removeFromBlacklist(reporter, key, blHost, removedTime);
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
		Set<String> blacklisted = eventsHandler.findCurrentlyBlacklistedBySource(reporter, key);

		for (String blValue : blacklisted) {
			if (!dirtyValues.contains(blValue)) {
				updated += eventsHandler.removeFromBlacklist(reporter, key, blValue, removedTime);
			}
		}
		
		return updated;
	}
}
