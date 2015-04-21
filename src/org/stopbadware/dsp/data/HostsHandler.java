package org.stopbadware.dsp.data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.sec.Permissions;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class HostsHandler extends MdbCollectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(HostsHandler.class);
	
	public HostsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDb.HOSTS));
		canRead = subject.isPermitted(Permissions.READ_HOSTS);
		canWrite = subject.isPermitted(Permissions.WRITE_HOSTS);
	}
	
	@Override
	protected DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException {
		DBObject critDoc = new BasicDBObject();
		for (String key : criteria.keySet()) {
			String value = criteria.getFirst(key);
			if (!value.isEmpty()) {
				switch (key	.toLowerCase()) {
					case "host":
						critDoc.put("host", new BasicDBObject("$regex", getRegex(value)));
						break;
					case "resolvesto":
						try {
							List<String> hosts = getHostsMostRecentlyMappedToIP(Long.valueOf(value));
							if (hosts.size() > 0) {
								critDoc.put("host", new BasicDBObject("$in", hosts));
							} else {
								/* If no hosts currently resolve to IP provided search 
								 * for empty string to return empty results instead of 
								 * no search criteria provided error 
								 */
								critDoc.put("host", "");
							}
						} catch (NumberFormatException e) {
							throw new SearchException("'"+value+"' is not a valid IP entry", Error.BAD_FORMAT);
						}
						break;
					case "hasresolvedto":
						try {
							critDoc.put("ips.ip", Long.valueOf(value));
						} catch (NumberFormatException e) {
							throw new SearchException("'"+value+"' is not a valid IP entry", Error.BAD_FORMAT);
						}
						break;
					default:
						break;
				}
			}
		}
		return critDoc;
	}
	
	public SearchResults getHost(String host) {
		return getSearchResult(new BasicDBObject("host", host.toLowerCase()));
	}
	
	public boolean addHost(String host, ShareLevel level) {
		if (host == null || host.length() < 1) {
			return false;
		}
		boolean wroteToDB = false;
		DBObject query = new BasicDBObject("host", host);
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(query);
			while (cur.hasNext()) {
                DBObject nextEntry = cur.next();
                LOG.info("Iterating over matching host entry, {}", nextEntry);
				String levelString = (String) nextEntry.get("share_level");
				if (levelString != null) {
					ShareLevel curLevel = ShareLevel.value(levelString);
					level = ShareLevel.getLeastRestrictive(curLevel, level);
				}
			}
		}
		
		DBObject updateDoc = new BasicDBObject();
		updateDoc.put("host", host);
		updateDoc.put("share_level", level.toString());
        DBObject setDoc = new BasicDBObject("$set",updateDoc);

		if (canWrite) {
			try {
				WriteResult wr = coll.update(query, setDoc, true, false);
				wroteToDB = wr.getN() > 0;
			} catch (MongoException e) {
				if (e.getCode() != DUPE_ERR) {
					LOG.error("Error writing host '{}' to database: {}", host, e.getMessage());
				}
			}
		}
		return wroteToDB;
	}
	
	public boolean addIpForHost(String host, long ip) {
		boolean wroteToDb = false;
		DBObject hostDoc = new BasicDBObject("host", host);
		DBObject ipDoc = new BasicDBObject();
		ipDoc.put("ip", ip);
		ipDoc.put("timestamp", System.currentTimeMillis()/1000);
		DBObject updateDoc = new BasicDBObject("$push", new BasicDBObject("ips", ipDoc));
		if (ipHasChanged(host, ip) && canWrite) {
			try {
                LOG.debug("Updating doc for host {}, with ip {}",host,ip);
				WriteResult wr = coll.update(hostDoc, updateDoc);
				wroteToDb = wr.getN() > 0;
			} catch (MongoException e) {
				if (e.getCode() != DUPE_ERR) {
					LOG.error("Error adding IP{} for host '{}': {}", ip, host, e.getMessage());
				}
			}
		}
		return wroteToDb;		
	}
	
	/**
	 * Finds all hosts that have the specified IP address as their most recent entry
	 * @param ip the IP address to match 
	 * @return a List of Strings containing the matching hosts
	 */
	private List<String> getHostsMostRecentlyMappedToIP(long ip) {
		List<String> hosts = new ArrayList<>();
		AggregationOutput aggr = null;
		if (canRead) {
			DBObject unwind = new BasicDBObject("$unwind", "$ips");
			DBObject projDoc = new BasicDBObject();
			projDoc.put("host", "$host");
			projDoc.put("ip", "$ips.ip");
			projDoc.put("ts", "$ips.timestamp");
			DBObject project = new BasicDBObject("$project", projDoc);
			DBObject sort = new BasicDBObject("$sort", new BasicDBObject("ts", 1));
			DBObject groupDoc = new BasicDBObject();
			groupDoc.put("_id", "$host");
			groupDoc.put("mostRecentIP", new BasicDBObject("$last", "$ip"));
			groupDoc.put("ts", new BasicDBObject("$last", "$ts"));
			DBObject group = new BasicDBObject("$group", groupDoc);
			DBObject match = new BasicDBObject("$match", new BasicDBObject("mostRecentIP", ip));
			aggr = coll.aggregate(unwind, project, sort, group, match);
			for (DBObject result : aggr.results()) {
				hosts.add(result.get("_id").toString());
			}
		}
		return hosts;
	}
	
	/**
	 * Checks if the most recent IP entry for the host differs from a potentially new entry
	 * @param host the host to check
	 * @param ip the new IP address
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean ipHasChanged(String host, long ip) {
        LOG.debug("ipHasChanged called, host = {}, ip = {}", host, ip);
		long mostRecentTimestamp = 0L;
		long mostRecentIP = -1;
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(new BasicDBObject("host", host), new BasicDBObject("ips", 1));
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
		LOG.debug("Most recent IP for host {} is {}.  Original IP is {}", host, ip, mostRecentIP);
		return (mostRecentIP != ip);
	}

    public Set<Long> getIPsForHost(String host) {
        Set<Long> matchingIPs = new HashSet<>();
        DBCursor cur = null;
        if (canRead) {
            cur = coll.find(new BasicDBObject("host", host), new BasicDBObject("ips", 1));
            LOG.info("IP search for host {} resulted in {} matches", host, cur.count());
        }

        while (cur != null && cur.hasNext()) {
            BasicDBObject nextOb = (BasicDBObject) cur.next();
            LOG.debug("nextOb = {}", nextOb);
            BasicDBList ips = (BasicDBList) nextOb.get("ips");
            LOG.debug("ips = {}", ips);
            if (ips != null) {
                for (String i : ips.keySet()) {
                    BasicDBObject ipOb = (BasicDBObject) ips.get(i);
                    Long ip = (long)ipOb.get("ip");
                    matchingIPs.add(ip);
                }
            }
        }
        return matchingIPs;
    }
}
