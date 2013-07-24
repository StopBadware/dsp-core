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
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

/**
 * Class to oversee handling of MongoDB database operations
 */
public class DBHandler {
	
	private DB db;
	private DBCollection asColl;
	private EventReportsHandler eventsHandler;
	private HostsHandler hostsHandler;
	private IPsHandler ipsHandler;
	private Subject subject; 
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	public static final String DUPE_ERR = "E11000";	//DELME?
	
	public enum SearchType {
		EVENT_REPORT,
		HOST,
		IP,
		AS
	}
	
	public DBHandler(Subject subject) {
		db = MongoDB.getDB();
		asColl = db.getCollection(MongoDB.ASNS);
		if (subject.isAuthenticated()) {
			this.subject = subject;
			eventsHandler = new EventReportsHandler(db, this.subject);
			hostsHandler = new HostsHandler(db, this.subject);
			ipsHandler = new IPsHandler(db, this.subject);
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
	 * Searches for documents matching the provided criteria
	 * @param criteria Map of search criteria
	 * @return SearchResults containing the matching documents
	 * @throws SearchException
	 */
	public SearchResults search(SearchType type, MultivaluedMap<String, String> criteria) throws SearchException {
		if (criteria == null || criteria.size() < 1) {
			throw new SearchException("Insufficient search criteria", Error.BAD_FORMAT);
		}
		
		SearchResults sr = null;
		switch (type) {
			case EVENT_REPORT:
				sr = eventsHandler.search(criteria);
				break;
			case HOST:
				sr = hostsHandler.search(criteria);
				break;
			case IP:
				sr = null;	//TODO: DATA-96
				break;
			case AS:
				break;
			default:
				break;
		}
		
		if (sr == null) {
			throw new SearchException("Invalid search type", Error.BAD_FORMAT);
		}
		
		return sr;
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
	 * Finds a specific Host
	 * @param hostname to find
	 * @return SearchResults containing the Host record
	 */
	public SearchResults findHost(String host) {
		return hostsHandler.getHost(host);
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
				ipsHandler.addIP(ip);
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
	 * Writes the ASNs associated with each IP in the map provided 
	 * @param asns a Map containing keys of IP addresses as longs and AS objects as values
	 * @return int: the number of inserted or updated documents
	 */
	public int addASNsForIPs(Map<Long, AutonomousSystem> asns) {
		int dbWrites = 0;
		Set<AutonomousSystem> as = new HashSet<>(asns.size());
		for (long ip : asns.keySet()) {
			as.add(asns.get(ip));
			dbWrites += ipsHandler.updateASN(ip, asns.get(ip).getAsn());
		}
		addAutonmousSystem(as);	//TODO: DATA-96 nice side effect bro this needs to be moved
		LOG.info("Associated {} Autonomous Systems with IP addresses", dbWrites);
		return dbWrites;		
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
