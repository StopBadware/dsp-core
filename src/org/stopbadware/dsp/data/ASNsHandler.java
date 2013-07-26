package org.stopbadware.dsp.data;

import java.util.List;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.sec.Permissions;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;

public class ASNsHandler extends MDBCollectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(ASNsHandler.class);
	
	public ASNsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDB.ASNS));
		canRead = subject.isPermitted(Permissions.READ_ASNS);
		canWrite = subject.isPermitted(Permissions.WRITE_ASNS);
	}

	@Override
	protected DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException {
		DBObject critDoc = new BasicDBObject();
		for (String key : criteria.keySet()) {
			String value = criteria.getFirst(key);
			if (!value.isEmpty()) {
				switch (key.toLowerCase()) {
					case "name":
						critDoc.put("name", new BasicDBObject("$regex", getRegex(value)));
						break;
					case "number":
						critDoc.put("asn", value);
						break;
					case "country":
						critDoc.put("country", value);
						break;
					default:
						break;
				}
			}
		}
		return critDoc;
	}
	
	public SearchResults getAS(int asn) {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			DBObject query = new BasicDBObject("asn", asn);
			List<DBObject> res = coll.find(query, hideKeys()).toArray();
			sr.setResults(res);
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	/**
	 * Adds an Autonomous System to database
	 * @param autonomousSystem the Autonomous Systems to add
	 * @return boolean: if a DB write occured 
	 */
	public boolean addAutonmousSystem(AutonomousSystem as) {
		boolean wroteToDB = false;
		int asn = as.getAsn();
		if (asn > 0) {
			DBObject doc = new BasicDBObject();
			doc.put("asn", asn);
			doc.put("country", as.getCountry());
			doc.put("name", as.getName());
			
			DBObject asnDoc = new BasicDBObject("asn", asn);
			
			if (canWrite) {
				WriteResult wr = coll.update(asnDoc, doc, true, false);
				if (wr.getError() != null && !wr.getError().contains(DUPE_ERR)) {
					LOG.error("Error writing ASN {} to collection: {}", asn, wr.getError());
				} else {
					wroteToDB = wr.getN() > 0;
				}
			}
		}
		return wroteToDB;
	}

}
