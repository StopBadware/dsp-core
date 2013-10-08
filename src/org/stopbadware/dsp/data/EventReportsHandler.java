package org.stopbadware.dsp.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler.WriteStatus;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.Domain;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class EventReportsHandler extends MdbCollectionHandler {
	
	private static final Logger LOG = LoggerFactory.getLogger(EventReportsHandler.class);
	
	public EventReportsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDb.EVENT_REPORTS));
		canRead = subject.isPermitted(Permissions.READ_EVENTS);
		canWrite = subject.isPermitted(Permissions.WRITE_EVENTS);
	}
	
	public SearchResults findEventReportsSince(long sinceTime) {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			DBObject query = new BasicDBObject("reported_at", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
			DBObject sort = new BasicDBObject("reported_at", ASC);
			List<DBObject> res = coll.find(query, hideKeys()).sort(sort).limit(MAX).toArray();
			sr.setResults(res);
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public SearchResults getEventReportsStats(String source) {
		SearchResults sr = null;
		if (canRead) {
			List<Map<String, Object>> results = new ArrayList<>();
			sr = new SearchResults();
			Map<String, Object> stats = new HashMap<>();
			stats.put("total_count", coll.getCount());
			stats.put("on_blacklist_count", coll.getCount(new BasicDBObject("is_on_blacklist", true)));
			long now = System.currentTimeMillis() / 1000;
			long dayAgo = now - SECONDS_IN_DAY;
			long weekAgo = now - (SECONDS_IN_DAY * 7);
			long monthAgo = now - (SECONDS_IN_DAY * 30);
			stats.put("added_last_1", getNumEventReportsAdded(dayAgo, now, source));
			stats.put("added_last_7", getNumEventReportsAdded(weekAgo, now, source));
			stats.put("added_last_30", getNumEventReportsAdded(monthAgo, now, source));
			results.add(stats);
			sr.setResults(results);
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public SearchResults getEventReport(String uid) throws SearchException {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			String[] tokens = uid.split("-");
			if (tokens.length != 3) {
				throw new SearchException("'"+uid+"' is not a valid Event Report ID", Error.BAD_FORMAT);
			} else {
				DBObject searchFor = new BasicDBObject();
				searchFor.put("sha2_256", tokens[0]);
				searchFor.put("prefix", tokens[1]);
				Long reportedAt = 0L;
				try {
					reportedAt = Long.valueOf(tokens[2]);
				} catch (NumberFormatException e) {
					throw new SearchException("'"+uid+"' is not a valid Event Report ID", Error.BAD_FORMAT);
				}
				searchFor.put("reported_at", reportedAt);
				List<DBObject> res = coll.find(searchFor, hideKeys()).limit(1).toArray();
				sr.setResults(res);
			}
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public TimeOfLast getTimeOfLast(String source) {
		long time = 0L;
		DBObject query = new BasicDBObject();
		Pattern sourceRegex = getRegex(source);
		query.put("prefix", sourceRegex);
		
		DBObject keys = new BasicDBObject();
		keys.put("_id", 0);
		keys.put("reported_at", 1);
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(query, keys).sort(new BasicDBObject ("reported_at", DESC)).limit(1);
		}
		while (cur != null && cur.hasNext()) {
			try {
				time = Long.valueOf(cur.next().get("reported_at").toString());
			} catch (NumberFormatException | NullPointerException e) {
				time = 0L;
			}
		}
		return new TimeOfLast(source, time);
	}
	
	public Set<String> getCurrentlyBlacklistedHosts() {
		Set<String> hosts = new HashSet<>();
		DBObject query = new BasicDBObject();
		query.put("is_on_blacklist", true);
		DBObject keys = new BasicDBObject();
		keys.put("_id", 0);
		keys.put("host", 1);
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(query, keys);
		}
		while (cur != null && cur.hasNext()) {
			try {
				hosts.add(cur.next().get("host").toString());
			} catch (MongoException | NullPointerException e) {
				LOG.error("Unable to retrieve object from cursor:\t{}", e.getMessage());
			}
		}
		return hosts;
	}
	
	@Override
	protected DBObject createCriteriaObject(MultivaluedMap<String, String> criteria) throws SearchException {
		DBObject critDoc = new BasicDBObject();
		DBObject reportedAt = new BasicDBObject();
		
		for (String key : criteria.keySet()) {
			String value = criteria.getFirst(key);
			if (!value.isEmpty()) {
				switch (key	.toLowerCase()) {
					case "url":
						critDoc.putAll(createURLDoc(value));
						break;
					case "scheme":
						critDoc.put("scheme", value);
						break;
					case "host":
						critDoc.put("host", value);
						break;
					case "path":
						String path = (value.startsWith("/")) ? value : "/"+value;
						critDoc.put("path", path);
						break;
					case "query":
						String query = (value.startsWith("?")) ? value.substring(1) : value;
						critDoc.put("query", query);
						break;
					case "reportedby":
						critDoc.put("prefix", value);
						break;
					case "reptype":
						critDoc.put("report_type", value);
						break;
					case "blacklist":
						if (value.equalsIgnoreCase("never")) {
							critDoc.put("report_type", new BasicDBObject("$ne", "BLACKLISTED"));
						} else if (value.equalsIgnoreCase("currently")) {
							critDoc.put("report_type", "BLACKLISTED");
							critDoc.put("is_on_blacklist", true);
						} else if (value.equalsIgnoreCase("previously")) {
							critDoc.put("report_type", "BLACKLISTED");
							critDoc.put("is_on_blacklist", false);
						} else {
							throw new SearchException("'"+value+"' is not a valid 'blacklist' value", Error.BAD_FORMAT);
						}
						break;
					case "after":
						try {
							reportedAt.put("$gte", Long.valueOf(value));
						} catch (NumberFormatException e) {
							throw new SearchException("'"+value+"' is not a valid 'after' value", Error.BAD_FORMAT);
						}
						break;
					case "before":
						try {
							reportedAt.put("$lte", Long.valueOf(value));
						} catch (NumberFormatException e) {
							throw new SearchException("'"+value+"' is not a valid 'before' value", Error.BAD_FORMAT);
						}
						break;
					default:
						break;
				}
			}
		}
		
		if (!reportedAt.toMap().isEmpty()) {
			critDoc.put("reported_at", reportedAt);
		}
		
		return critDoc;
	}
	
	private DBObject createURLDoc(String url) {
		DBObject urlDoc = new BasicDBObject();
		if (url.matches("^\\w+://.*")) {
			urlDoc.put("sha2_256", SHA2.get256(url));
		} else {
			List<DBObject> orList = new ArrayList<>();
			orList.add(new BasicDBObject("sha2_256", SHA2.get256(url)));
			if (url.contains("/")) {
				int firstSlash = url.indexOf("/");
				String host = url.substring(0, firstSlash);
				String path = url.substring(firstSlash);
				DBObject hostAndPath = new BasicDBObject();
				hostAndPath.put("host", host);
				hostAndPath.put("path", path);
				orList.add(hostAndPath);
			} else {
				List<DBObject> hostOr = new ArrayList<>();
				hostOr.add(new BasicDBObject("host", url));
				String reversed = "^"+Domain.reverseDomain(url)+"\\..+$";
				hostOr.add(new BasicDBObject("reversed_host", new BasicDBObject("$regex", reversed)));
				orList.add(new BasicDBObject("$or", hostOr));
			}
			urlDoc.put("$or", orList);
		}
		return urlDoc;
	}
	
	private long getNumEventReportsAdded(long start, long end, String source) {
		long cnt = 0;
		if (canRead) {
			DBObject timeframe = new BasicDBObject();
			timeframe.put("$gte", start);
			timeframe.put("$lt", end);
			DBObject search = new BasicDBObject("reported_at", timeframe);
			if (!source.equalsIgnoreCase("all")) {
				search.put("prefix", source);
			}
			cnt = coll.getCount(search);
		}
		return cnt;
	}
	
	/**
	 * Inserts a single Event Report into database, ignoring duplicates.
	 * @param event key/value map to be inserted
	 * @return WriteResult: the WriteResult associated with the write attempt
	 * or null if the attempt was unsuccessful
	 */
	public WriteStatus addEventReport(Map<String, Object> event) {
		WriteStatus status = WriteStatus.FAILURE;
		
		DBObject doc = new BasicDBObject();
		doc.putAll(event);
		doc.put("_created", System.currentTimeMillis() / 1000);
		
		if (canWrite) {
			try {
				WriteResult wr = coll.insert(doc);
				if (wr.getError() != null) {
					if (wr.getError().contains(DUPE_ERR)) {
						status = WriteStatus.DUPLICATE;
					} else	{
						if (doc.get("url") != null) {
							LOG.error("Error writing {} report to collection: {}", doc.get("url"), wr.getError());
						} else {
							LOG.error("Error writing report with null URL to collection: {}", wr.getError());
						}
					}
				} else {
					status = WriteStatus.SUCCESS;
				}
			} catch (MongoException e) {
				LOG.error("MongoException thrown when adding event report:\t{}", e.getMessage());
			}
		}
		
		return status;
	}
	
	/**
	 * Find the event reports currently marked as blacklisted for the reporter provided
	 * @param reporter prefix for the blacklisting source
	 * @param field the key to retrieve from each report
	 * @return a Set of Strings containing the value for the corresponding key
	 */
	public Set<String> findCurrentlyBlacklistedBySource(String reporter, String field) {
		DBObject query = new BasicDBObject();
		query.put("prefix", getRegex(reporter));
		query.put("is_on_blacklist", true);
		DBObject keys = new BasicDBObject(field, 1);
		keys.put("_id", 0);
		DBCursor cur = null;
		if (canRead) {
			cur = coll.find(query, keys);
		}
		Set<String> blacklisted = new HashSet<>();

		while (cur != null && cur.hasNext()) {
			Object val = cur.next().get(field);
			if (val != null) {
				blacklisted.add(val.toString());
			}
		}
		
		return blacklisted;
	}
	
	/**
	 * Updates event reports for the specified reporter matching the provided key/value 
	 * to have an is_on_blacklist flag of false and sets the removed_from_blacklist time
	 * @param reporter prefix of the reporting entity
	 * @param key db document field to match
	 * @param value matching value
	 * @param removedTime UNIXTimestamp as a long to set as the removed time
	 * @return int: the number of event reports updated
	 */
	public int removeFromBlacklist(String reporter, String key, Object value, long removedTime) {
		int updated = 0;
		DBObject query = new BasicDBObject();
		query.put(key, value);
		query.put("prefix", reporter);
		query.put("is_on_blacklist", true);
		
		DBObject update = new BasicDBObject();
		update.put("is_on_blacklist", false);
		update.put("removed_from_blacklist", removedTime);
		WriteResult wr = null;
		if (canWrite) {
			wr = coll.update(query, new BasicDBObject("$set", update), false, true);
		}
		if (wr != null) {
			if (wr.getError() != null) {
				LOG.error("Error changing blacklist flag for {}:\t{}", value, wr.getError());
			} else if (wr.getError() == null) {
				updated += wr.getN();
			}
		}
		
		return updated;
	}
	
}
