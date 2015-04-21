package org.stopbadware.dsp.data;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.RateLimitException;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.AuthAuth;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.DB;

/**
 * Class to oversee handling of MongoDB database operations
 */
public class DbHandler {
	
	private DB db;
	private EventReportsHandler eventsHandler;
	private HostsHandler hostsHandler;
	private IpsHandler ipsHandler;
	private AsnsHandler asnsHandler;
	private Subject subject; 
	private static final Logger LOG = LoggerFactory.getLogger(DbHandler.class);

    Object queueLock = new Object();
    Set<ERWrapper> combinedReports = new HashSet<>();

    private static DbHandler systemDBHandler;

    public static void startSystemThread() {
		Subject systemSubject = AuthAuth.createSystemSubject();
		if(!systemSubject.isAuthenticated()) {
			LOG.error("System subject is not authenticated.  Make sure admin user is properly setup.");
		} else {
			systemDBHandler = new DbHandler(systemSubject);
			systemDBHandler.startEventReportUpdatingThread();
		}
    }

    public void queueNewEventReport(Set<ERWrapper> ers) {
        synchronized (queueLock) {
            combinedReports.addAll(ers);
        }
    }
    public void startEventReportUpdatingThread() {
        new Thread() {
            @Override
            public void run() {
                LOG.info("Thread executing");
                while (true) {
					LOG.info("Iterating in event report updating thread.");
                    try {
                        Thread.sleep(10000);
                        synchronized (queueLock) {
							LOG.info("Combined reports size = {}", combinedReports.size());
							if(!combinedReports.isEmpty()) {
								LOG.info(combinedReports.size()+" eventReports need extra info added.");
								addIPsToEventReports(combinedReports);
								combinedReports.clear();
							}
                        }
                    } catch (InterruptedException e) {
                        if (LOG.isErrorEnabled()) {
                            String hosts = "";
                            synchronized (queueLock) {
                                for (ERWrapper er : combinedReports) {
                                    hosts += er.getHost() + ", ";
                                }
                                LOG.error("IP lookup thread interrupted, queue = " + combinedReports.toString(), e);
                            }
                        }
                    }
                }
            }
        }.start();
    }


    private void addIPsToEventReports(Set<ERWrapper> reports) {
        Map<String,Set<Long>> hostIPMap = new HashMap<>();
		int found = 0;
		int unique = 0;
		Set<String> hosts = new HashSet<>();
		for(ERWrapper er: reports) {
		    if(!hosts.contains(er.getHost()))
				hosts.add(er.getHost());
		}
        for(String host: hosts) {
			Set<Long> ips = getIPsForHost(host);
			hostIPMap.put(host, ips);
			if (ips.size() > 0) {
				found++;
				LOG.debug("IPs {} will be added to event report {}", ips, host);
			}
        }
		LOG.info("{} successful IP lookups for {} unique hosts in {} reports", found, hosts.size(), reports.size());
        for(String host: hostIPMap.keySet()) {
			Set<Long> ips = hostIPMap.get(host);
			WriteStatus status = eventsHandler.addIPsToEventReport(host, ips);
			if(status != WriteStatus.SUCCESS) {
				LOG.error("Could not add IPs to event report. WriteStatus = {}, host = {}, ips = {}", host, ips);
			} else {
				LOG.info("IP addresses added to event reports with host {}",host);
			}
        }
    }

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

    public DbHandler(Subject subject) {
		db = MongoDb.getDB();
		if (subject.isAuthenticated() ) {
			this.subject = subject;
			eventsHandler = new EventReportsHandler(db, this.subject);
			hostsHandler = new HostsHandler(db, this.subject);
			ipsHandler = new IpsHandler(db, this.subject);
			asnsHandler = new AsnsHandler(db, this.subject);
		}
	}
	
	/**
	 * Retrieves a map of prefixes and their corresponding participant (full) names
	 * for participants with at least one event report associated with their prefix
	 * @return SearchResults containing a map of prefix=>participant mappings
	 */
	public SearchResults getParticipantPrefixes() {
		return eventsHandler.getParticipantPrefixes();
	}
	
	/**
	 * Finds Event Reports added since the specified timestamp or UID, 
	 * up to a maximum of 25K sorted by time ascending
	 * @param since String representing either a UNIX timestamp or a valid UID
	 * @return SearchResults with the results
	 * @throws SearchException
	 * @throws RateLimitException 
	 */
	public SearchResults findEventReportsSince(String since) throws SearchException, RateLimitException {
		try {
			return eventsHandler.findEventReportsSince(Long.valueOf(since));
		} catch (NumberFormatException e) {
			return eventsHandler.findEventReportsSince(since);
		}
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
	 * @throws RateLimitException 
	 */
	public SearchResults search(SearchType type, MultivaluedMap<String, String> criteria) throws SearchException, RateLimitException {
		if (criteria == null || criteria.size() < 1) {
			throw new SearchException("Insufficient search criteria", Error.BAD_FORMAT);
		}
		if (AuthAuth.isRateLimited(subject)) {
			throw new RateLimitException();
		}
		MdbCollectionHandler handler = null;
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
	 * @param UID (MongoDB generated ID) of the Event Report to find
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
     * Finds Event Reports potentially from a specified IP address
     * @param ip String containing IP address in either dot notation, or as a long
	 * @return SearchResults containing the IP record
     * @throws SearchException
     * @throws RateLimitException
     */

    public SearchResults findEventReportsWithIP(String ip) throws SearchException, RateLimitException {
		return (ip.matches("^\\d+$")) ? eventsHandler.getIp(Long.valueOf(ip)) : eventsHandler.getIp(ip);
	}
	
	/**
	 * Finds a specific Autonomous System
	 * @param AS number of the AS to find
	 * @return SearchResults containing the AS record
	 */
	public SearchResults findAS(int asn) {
		return asnsHandler.getAutonomousSystem(asn);
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
	 * Finds all hosts of currently blacklisted event reports
	 * @return Set of Strings containing the blacklisted hosts
	 */
	public Set<String> getCurrentlyBlacklistedHosts() {
		return eventsHandler.getCurrentlyBlacklistedHosts();
	}
	
	/**
	 * Finds all hosts corresponding to event reports added
	 * since the time specified
	 * @param since start time as a UNIX timestamp
	 * @return Set of Strings containing the hosts
	 */
	public Set<String> getHostsWithEventReportsSince(long since) {
		return eventsHandler.getHostsWithEventReportsSince(since);
	}
	
	/**
	 * Inserts multiple Event Reports into database, ignoring duplicates.
	 * @param reports a set of key/value maps to be inserted 
	 * @return int: number of new documents written to database plus 
	 * duplicate documents that were ignored 
	 */
	public int addEventReports(Set<ERWrapper> reports) {	
		int dbInserts = 0;
		int dbUpdates = 0;
		int dbDupes = 0;
		Set<ERWrapper> toAugment = new HashSet<ERWrapper>();
		for (ERWrapper er : reports) {
			if (er.getErMap() != null && er.getErMap().size() > 0) {
				WriteStatus status = eventsHandler.addEventReport(er.getErMap());
				switch(status) {
					case DUPLICATE:
						dbDupes++;
						break;
					case UPDATED:
						dbUpdates++;
						toAugment.add(er);
						break;
					case SUCCESS:
						dbInserts++;
						toAugment.add(er);
						break;
					default:
						break;
				}
			}
			addHost(er.getHost(), ShareLevel.value(er.getShareLevel()));
		}
		systemDBHandler.queueNewEventReport(toAugment);
		LOG.info("Queued {} event reports for info augmentation.",toAugment.size());
		LOG.info("{} new event reports added", dbInserts);
		LOG.info("{} existing event reports updated", dbUpdates);
		LOG.info("{} duplicate entries ignored", dbDupes);
		return dbInserts + dbUpdates + dbDupes;
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
				ipsHandler.addIp(ip);
			}
			boolean addedOrUpdatedIP = hostsHandler.addIpForHost(host, ip);
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
	public int addAsnsForIps(Map<Long, AutonomousSystem> asns) {
		int dbWrites = 0;
		for (long ip : asns.keySet()) {
			dbWrites += ipsHandler.updateAsn(ip, asns.get(ip).getAsn());
		}
		LOG.info("Added or updated Autonomous System info for {} IP addresses", dbWrites);
		return dbWrites;		
	}
	
	/**
	 * Adds Autonomous Systems to database
	 * @param autonomousSystems the set containing the Autonomous Systems to add
	 * @return int: the number of inserted or updated documents
	 */
	public int addAutonmousSystems(Set<AutonomousSystem> autonomousSystems) {
		int addedOrUpdated = 0;
		for (AutonomousSystem as : autonomousSystems) {
			boolean success = asnsHandler.addAutonmousSystem(as);
			if (success) {
				addedOrUpdated++;
			}
		}
		LOG.info("Added or updated {} Autonomous Systems ({} expected)", addedOrUpdated, autonomousSystems.size());
		return addedOrUpdated;
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
		
		return (set.size() > 0) ? updateBlacklistFlagsFromDirty(reporter, removedTime, sha2urls, key) : 0;
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

    public Set<Long> getIPsForHost(String host) {
        return hostsHandler.getIPsForHost(host);
    }
}
