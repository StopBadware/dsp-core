package org.stopbadware.dsp.data;

import com.mongodb.*;
import org.apache.shiro.subject.Subject;
import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.sec.Permissions;

import javax.ws.rs.core.MultivaluedMap;

public class WhoisHandler extends MdbCollectionHandler {

	private static final Logger LOG = LoggerFactory.getLogger(WhoisHandler.class);

	public WhoisHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDb.WHOIS));
		canRead = true;
		canWrite = true;
	}


	@Override
	protected DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException {
		DBObject critDoc = new BasicDBObject();
		for (String key : criteria.keySet()) {
			String value = criteria.getFirst(key);
			if (!value.isEmpty()) {
				switch (key.toLowerCase()) {
					case "host":
						critDoc.put("host", new BasicDBObject("$regex", getRegex(value)));
						break;
					default:
						break;
				}
			}
		}
		return critDoc;
	}

	public SearchResults getWhois(String host) {
		SearchResults sr = null;
		if (canRead) {
 			sr = getSearchResult(new BasicDBObject("host", host));
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	/**
	 * Adds Whois info to database
	 * @param host the host to add whois info for
	 * @param whois the whois info of the host
	 * @return boolean: if a DB write occurred
	 */
	public boolean upsertWhois(String host, String whois) {
		boolean wroteToDb = false;
		DBObject doc = new BasicDBObject();
		doc.put("host", host);
		doc.put("whois", whois);
		doc.put("_created", System.currentTimeMillis() / 1000);

		DBObject hostDoc = new BasicDBObject("host", host);

		if (canWrite) {
			try {
				WriteResult wr = coll.update(hostDoc, doc, true, false);
				wroteToDb = wr.getN() > 0;
			} catch (MongoException e) {
				if (e.getCode() != DUPE_ERR) {
					LOG.error("Error writing host {} to database: {}", host, e.getMessage());
				}
			}
		}
		return wroteToDb;
	}

}
