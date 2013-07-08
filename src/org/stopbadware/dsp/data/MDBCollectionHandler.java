package org.stopbadware.dsp.data;

import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public abstract class MDBCollectionHandler {
	
	protected DB db;
	protected DBCollection coll;
	protected boolean canRead = false;
	protected boolean canWrite = false;
	
	protected static final String DUPE_ERR = "E11000";
	protected static final int ASC = MongoDB.ASC;
	protected static final int DESC = MongoDB.DESC;
	
	private static final Logger LOG = LoggerFactory.getLogger(MDBCollectionHandler.class);
	
	protected MDBCollectionHandler(DB db, DBCollection coll) {
		this.db = db;
		this.coll = coll;
	}
	
	/**
	 * Convenience method for creating an exact case insensitive regex Pattern
	 * @param str String to match
	 * @return java.util.regex Pattern or null if could not create Pattern
	 */
	protected Pattern getRegex(String str) {
		Pattern p = null;
		try {
			p = Pattern.compile("^" + str + "$", Pattern.MULTILINE|Pattern.CASE_INSENSITIVE);
		} catch (IllegalArgumentException e) {
			LOG.warn("Unable to create Pattern for >>{}<<\t{}", str, e.getMessage());
		}
		return p;
	}
	
	protected DBObject hideKeys(String... keysToHide) {
		DBObject keys = new BasicDBObject("_id", 0);
		keys.put("_created", 0);
		for (String key : keysToHide) {
			keys.put(key, 0);
		}
		return keys;
	}

}
