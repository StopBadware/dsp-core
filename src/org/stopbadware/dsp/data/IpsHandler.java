package org.stopbadware.dsp.data;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.IP;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class IpsHandler extends MdbCollectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(IpsHandler.class);

	public IpsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDb.IPS));
		canRead = subject.isPermitted(Permissions.READ_IPS);
		canWrite = subject.isPermitted(Permissions.WRITE_IPS);
	}

	@Override
	protected DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException {
		DBObject critDoc = new BasicDBObject();
		for (String key : criteria.keySet()) {
			String value = criteria.getFirst(key);
			if (!value.isEmpty()) {
				switch (key.toLowerCase()) {
					case "ip":
						long ip = (value.matches("^\\d+$")) ? Long.valueOf(value) : IP.dotsToLong(value);
						critDoc.put("ip", ip);
						break;
					default:
						break;
				}
			}
		}
		return critDoc;
	}
	
	public SearchResults getIP(String ipDots) {
		return getIP(IP.dotsToLong(ipDots));
	}
	
	public SearchResults getIP(long ip) {
		return getSearchResult(new BasicDBObject("ip", ip));
	}
	
	/**
	 * Adds an IP address to the database
	 * @param ip the IP address to add
	 */
	public boolean addIP(long ip) {
		boolean wroteToDB = false;
		if (canWrite) {
			WriteResult wr = coll.insert(new BasicDBObject("ip", ip));
			if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
				LOG.error("Error writing {} / {} to database", ip, IP.longToDots(ip));
			} else {
				wroteToDB = true;
			}
		}
		return wroteToDB;
	}
	
	/**
	 * Adds the provided ASN to the IP's AS history if that ASN is not already
	 * the most recent entry
	 * @param ip the IP to update the ASN for
	 * @param asn the new ASN
	 * @return the number of documents updated
	 */
	public int updateASN(long ip, int asn) {
		int updated = 0;
		DBObject ipDoc = new BasicDBObject("ip", ip);
		if (coll.find(ipDoc).count() == 0) {
			addIP(ip);
		}
		if (canWrite && asnHasChanged(ip, asn)) {
			DBObject asnDoc = new BasicDBObject();
			asnDoc.put("asn", asn);
			asnDoc.put("timestamp", System.currentTimeMillis()/1000L);
			
			DBObject updateDoc = new BasicDBObject();
			updateDoc.put("$push", new BasicDBObject("asns", asnDoc));
			
			WriteResult wr = coll.update(ipDoc, updateDoc);
			if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
				LOG.error("Error writing to collection: {}", wr.getError());
			} else  {
				updated += wr.getN();
			}
		}
		return updated;
	}
	
	/**
	 * Checks if the most recent AS entry for the IP differs from a potentially new entry
	 * @param ip the IP to check
	 * @param asn the new ASN
	 * @return boolean, true if the new ASN differs from the most recent db entry
	 */
	private boolean asnHasChanged(long ip, int asn) {
		long mostRecentTimestamp = 0L;
		int mostRecentASN = 0;
		
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(new BasicDBObject("ip", ip), new BasicDBObject("asns", 1));
		}
		
		while (cur != null && cur.hasNext()) {
			BasicDBList asns = (BasicDBList) ((BasicDBObject) cur.next()).get("asns");
			if (asns != null && asns.size() > 0) {
				for (String as : asns.keySet()) {
					long timestamp = 0L;
					try {
						timestamp =  (long) ((BasicDBObject) asns.get(as)).get("timestamp");
					} catch (ClassCastException e) {
						/*Skip if cannot determine time*/
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
		
		return mostRecentASN != asn;
	}
}
