package org.stopbadware.dsp.data;

import java.util.Collection;
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
import org.stopbadware.lib.util.SHA2;

import com.mongodb.DB;

/**
 * Class to oversee handling of MongoDB database operations
 */
public class DBHandler {
	
	private DB db;
	private EventReportsHandler eventsHandler;
	private HostsHandler hostsHandler;
	private IPsHandler ipsHandler;
	private ASNsHandler asnsHandler;
	private Subject subject; 
	private static final Logger LOG = LoggerFactory.getLogger(DBHandler.class);
	
	public enum SearchType {
		EVENT_REPORT,
		HOST,
		IP,
		AS
	}
	
	public enum WriteStatus {
		SUCCESS,
		FAILURE,
		UPDATED,
		DUPLICATE
	}
	
	public DBHandler(Subject subject) {
		db = MongoDB.getDB();
		if (subject.isAuthenticated()) {
			this.subject = subject;
			eventsHandler = new EventReportsHandler(db, this.subject);
			hostsHandler = new HostsHandler(db, this.subject);
			ipsHandler = new IPsHandler(db, this.subject);
			asnsHandler = new ASNsHandler(db, this.subject);
		}
	}
	
	/**
	 * Finds Event Reports since the specified timestamp, up to a maximum of 25K
	 * sorted by reported at time ascending
	 * @param sinceTime UNIX timestamp to retrieve reports since
	 * @return SearchResults with the results
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
		
		MDBCollectionHandler handler = null;
		switch (type) {
			case EVENT_REPORT:
				handler = eventsHandler; 
				break;
			case HOST:
				handler = hostsHandler;
				break;
			case IP:
				handler = ipsHandler;
				break;
			case AS:
				handler = asnsHandler;
				break;
			default:
				break;
		}
		
		if (handler == null) {
			throw new SearchException("Invalid search type", Error.BAD_FORMAT);
		} else {
			return handler.search(criteria);
		}
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
	 * Finds a specific Host
	 * @param hostname to find
	 * @return SearchResults containing the Host record
	 */
	public SearchResults findHost(String host) {
		return hostsHandler.getHost(host);
	}
	
	/**
	 * Finds a specific IP address
	 * @param IP address to find
	 * @return SearchResults containing the IP record
	 */
	public SearchResults findIP(String ip) {
		return (ip.matches("^\\d+$")) ? ipsHandler.getIP(Long.valueOf(ip)) : ipsHandler.getIP(ip);
	}
	
	/**
	 * Finds a specific Autonomous System
	 * @param AS number of the AS to find
	 * @return SearchResults containing the AS record
	 */
	public SearchResults findAS(int asn) {
		return asnsHandler.getAS(asn);
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
			if (er.getErMap() != null && er.getErMap().size() > 0) {
				WriteStatus status = eventsHandler.addEventReport(er.getErMap());
				if (status == WriteStatus.DUPLICATE) {
					dbDupes++;
				} else if (status == WriteStatus.SUCCESS) {
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
		for (long ip : asns.keySet()) {
			dbWrites += ipsHandler.updateASN(ip, asns.get(ip).getAsn());
		}
		LOG.info("Associated {} Autonomous Systems with IP addresses", dbWrites);
		return dbWrites;		
	}
	
	/**
	 * Adds Autonomous Systems to database
	 * @param autonomousSystems the set containing the Autonomous Systems to add
	 * @return int: the number of inserted or updated documents
	 */
	public int addAutonmousSystems(Collection<AutonomousSystem> autonomousSystems) {
		int dbWrites = 0;
		for (AutonomousSystem as : autonomousSystems) {
			boolean addedOrUpdated = asnsHandler.addAutonmousSystem(as);
			if (addedOrUpdated) {
				dbWrites++;
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
