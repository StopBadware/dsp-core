package org.stopbadware.dsp.data;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.IP;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBObject;

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

}
