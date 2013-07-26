package org.stopbadware.dsp.data;

import java.util.List;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;
import com.mongodb.MongoException;

public abstract class MDBCollectionHandler {
	
	protected DB db;
	protected DBCollection coll;
	protected boolean canRead = false;
	protected boolean canWrite = false;
	
	protected static final String DUPE_ERR = "E11000";
	protected static final int SECONDS_IN_DAY = 60 * 60 * 24;
	protected static final int ASC = MongoDB.ASC;
	protected static final int DESC = MongoDB.DESC;
	protected static final int MAX = 25000;
	
	private static final Logger LOG = LoggerFactory.getLogger(MDBCollectionHandler.class);
	
	protected MDBCollectionHandler(DB db, DBCollection coll) {
		this.db = db;
		this.coll = coll;
	}
	
	protected abstract DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException;
	
	/**
	 * Convenience method for returning an appropriate error response when the subject
	 * is not authorized to access the requested resource
	 * @return
	 */
	protected SearchResults notPermitted() {
		return new Error(Error.NOT_PERMITTED, "Your account is not permitted to access the requested resource");
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
	
	/**
	 * Hides the MongoDB _id key and additional keys if provided
	 * @param keysToHide an optional array of field names to hide
	 * @return DBObject to include as the keys parameter
	 */
	protected DBObject hideKeys(String... keysToHide) {
		DBObject keys = new BasicDBObject("_id", 0);
		keys.put("_created", 0);
		for (String key : keysToHide) {
			keys.put(key, 0);
		}
		return keys;
	}
	
	/**
	 * Performs a search for the provided query
	 * @param query DBObject with the search criteria
	 * @return SearchResults containing all matching documents 
	 */
	protected SearchResults getSearchResult(DBObject query) {
		SearchResults sr = null;
		if (canRead) {
			try {
				sr = new SearchResults();
				List<DBObject> res = coll.find(query, hideKeys()).toArray();
				sr.setResults(res);
			} catch (MongoException e) {
				LOG.error("Unable to execute query: {}", e.getMessage());
				sr = new Error(Error.DATABASE);
			}
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	/**
	 * Performs a search on the collection
	 * @param criteria MultivaluedMap with the criteria to search for
	 * @return SearchResults containing the documents matching the search criteria 
	 * @throws SearchException
	 */
	public SearchResults search(MultivaluedMap<String, String> criteria) throws SearchException {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			DBObject searchFor = createCriteriaObject(criteria);
			if (searchFor.keySet().size() < 1) {
				throw new SearchException("No search criteria specified", Error.BAD_FORMAT);
			} else {
				List<DBObject> res = coll.find(searchFor, hideKeys()).limit(MAX).toArray();
				sr.setResults(res);
			}
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	

}
