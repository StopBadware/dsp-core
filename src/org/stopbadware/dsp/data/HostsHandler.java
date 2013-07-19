package org.stopbadware.dsp.data;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.sec.Permissions;

import com.mongodb.AggregationOutput;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class HostsHandler extends MDBCollectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(HostsHandler.class);
	
	public HostsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDB.HOSTS));
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
					case "matches":
						critDoc.put("host", new BasicDBObject("$regex", value));
						break;
					case "resolvesto":
						try {
							//TODO
							//"1128.atraxio.com"
							//3286932030
							//3286932031 <- MOST RECENT
							System.out.println(value);		//DELME: DATA-96
							getMostRecentIP("1128.atraxio.com");	//DELME: DATA-96
//							critDoc.put("$match", new BasicDBObject("mostRecentIP", Long.valueOf(value)));
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
				String levelString = (String) cur.next().get("share_level");
				if (levelString != null) {
					ShareLevel curLevel = ShareLevel.castFromString(levelString);
					level = ShareLevel.getLeastRestrictive(curLevel, level);
				}
			}
		}
		
		DBObject updateDoc = new BasicDBObject();
		updateDoc.put("host", host);
		updateDoc.put("share_level", level.toString());

		WriteResult wr = null;
		if (canWrite) {
			wr = coll.update(query, updateDoc, true, false);
		}
		if (wr != null && wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
			LOG.error("Error writing {} to host collection: {}", host, wr.getError());
		} else {
			wroteToDB = true;
		}
		return wroteToDB;	
	}
	
	public boolean addIPForHost(String host, long ip) {
		boolean wroteToDB = false;
		DBObject hostDoc = new BasicDBObject();
		hostDoc.put("host", host);
		
		
		DBObject ipDoc = new BasicDBObject();
		ipDoc.put("ip", ip);
		ipDoc.put("timestamp", System.currentTimeMillis()/1000);
		
		DBObject updateDoc = new BasicDBObject();
		updateDoc.put("$push", new BasicDBObject("ips", ipDoc));
		if (ipHasChanged(host, ip)) {
			if (canWrite) {
				WriteResult wr = coll.update(hostDoc, updateDoc);
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					LOG.error("Error writing to collection: {}", wr.getError());
				} else  {
					wroteToDB = wr.getN() > 0;
				}
			}
		}
		return wroteToDB;		
	}
	
	private long getMostRecentIP(String host) {
		long mostRecentIP = 0L;
		AggregationOutput aggr = null;
		if (canRead) {
			DBObject unwind = new BasicDBObject();
			DBObject project = new BasicDBObject();
			DBObject sort = new BasicDBObject();
			DBObject group = new BasicDBObject();
			DBObject match = new BasicDBObject();
			aggr = coll.aggregate(match, unwind, project, sort, group);
			for (DBObject result : aggr.results()) {
				System.out.println(result);	//DELME
			}
		}
		return mostRecentIP;
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
		
		return (mostRecentIP != ip);
	}

}
