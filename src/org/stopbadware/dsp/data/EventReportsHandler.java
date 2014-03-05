package org.stopbadware.dsp.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler.WriteStatus;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.AuthAuth;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.lib.util.Domain;
import org.stopbadware.lib.util.SHA2;
import org.bson.types.ObjectId;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

public class EventReportsHandler extends MdbCollectionHandler {
	
	private Subject subject;
	private static final Logger LOG = LoggerFactory.getLogger(EventReportsHandler.class);
	
	public EventReportsHandler(DB db, Subject subject) {
		super(db, db.getCollection(MongoDb.EVENT_REPORTS));
		this.subject = subject;
		canRead = subject.isPermitted(Permissions.READ_EVENTS);
		canWrite = subject.isPermitted(Permissions.WRITE_EVENTS);
	}
	
	public SearchResults getParticipantPrefixes() {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			Map<String, String> prefixes = new HashMap<>();
			DBObject groupBy = new BasicDBObject();
			groupBy.put("prefix", 1);
			groupBy.put("reported_by", 1);
			DBObject results = coll.group(groupBy, new BasicDBObject(), new BasicDBObject(), "function() {}");
			for (Object key : results.keySet()) {
				Object obj = results.get(key.toString());
				if (obj instanceof DBObject) {
					DBObject doc = (DBObject) results.get(key.toString());
					Object prefix = doc.get("prefix");
					Object reportedBy = doc.get("reported_by");
					if (prefix != null && reportedBy != null) {
						prefixes.put(prefix.toString(), reportedBy.toString());
					}
				}
			}
			List<Map<String, String>> resultList = new ArrayList<>();
			resultList.add(prefixes);
			sr.setResults(resultList);
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public SearchResults findEventReportsSince(long sinceTime) {
		SearchResults sr = null;
		if (canRead) {
			if (AuthAuth.isRateLimited(subject)) {
				sr = notPermitted();	//TODO DATA-122 return rate limit error
			} else {
				sr = new SearchResults();
				DBObject query = new BasicDBObject("_created", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
				sr.setResults(findAndSetUid(query));
			}
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public SearchResults findEventReportsSince(String sinceReport) throws SearchException {
		try {
			return findEventReportsSince(Long.valueOf(sinceReport.substring(0, 8), 16));
		} catch (NumberFormatException | IndexOutOfBoundsException e) {
			throw new SearchException("'"+sinceReport+"' is not a valid Event Report ID", Error.BAD_FORMAT);
		}
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
			try {
				sr.setResults(findAndSetUid(new BasicDBObject("_id", new ObjectId(uid))));
			} catch (IllegalArgumentException e) {
				throw new SearchException("'"+uid+"' is not a valid Event Report ID", Error.BAD_FORMAT);
			}
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public SearchResults getEventReport(String prefix, String sha256, long reportedAt) {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults();
			DBObject query = new BasicDBObject();
			query.put("prefix", prefix);
			query.put("sha2_256", sha256);
			query.put("reported_at", reportedAt);
			sr.setResults(findAndSetUid(query));
		} else {
			sr = notPermitted();
		}
		return sr;
	}
	
	public TimeOfLast getTimeOfLast(String prefix) {
		long time = 0L;
		DBObject query = new BasicDBObject();
		query.put("prefix", prefix.toLowerCase());
		
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
		return new TimeOfLast(prefix, time);
	}
	
	public Set<String> getCurrentlyBlacklistedHosts() {
		return findSingleFieldResults(new BasicDBObject("is_on_blacklist", true), "host");
	}
	
	public Set<String> getHostsWithEventReportsSince(long since) {
		DBObject query = new BasicDBObject("reported_at", new BasicDBObject("$gte", since));
		return findSingleFieldResults(query, "host");
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
						critDoc.putAll(createUrlDoc(value));
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
	
	private DBObject createUrlDoc(String url) {
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
	
	private List<DBObject> findAndSetUid(DBObject query) {
		List<DBObject> results = coll.find(query, new BasicDBObject("_created", 0)).sort(new BasicDBObject("_created", ASC)).limit(MAX).toArray();
		for (DBObject result : results) {
			result.put("uid", result.get("_id").toString());
			result.removeField("_id");
		}
		return results;
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
				status = (wr.getError()==null) ? WriteStatus.SUCCESS : WriteStatus.FAILURE;
			} catch (MongoException e) {
				if (e.getCode() == DUPE_ERR) {
					if (blacklistFieldsHaveChanged(event)) {
						boolean updated = updateBlacklistFields(event);
						status = (updated) ? WriteStatus.UPDATED : WriteStatus.DUPLICATE;
					} else {
						status = WriteStatus.DUPLICATE;
					}
				} else {
					if (doc.get("url") != null) {
						LOG.error("Error writing '{}' report to collection: {}", doc.get("url"), e.getMessage());
					} else {
						LOG.error("Error writing report with null URL to collection: {}", e.getMessage());
					}
				}
			}
		}
		
		return status;
	}
	
	private boolean updateBlacklistFields(Map<String, Object> event) {
		boolean updated = false;
		if (canWrite && event.containsKey("removed_from_blacklist") && event.containsKey("is_on_blacklist")) {
			DBObject doc = new BasicDBObject();
			doc.put("sha2_256", event.get("sha2_256"));
			doc.put("prefix", event.get("prefix"));
			doc.put("reported_at", event.get("reported_at"));
			
			DBObject set = new BasicDBObject();
			set.put("removed_from_blacklist", event.get("removed_from_blacklist"));
			set.put("is_on_blacklist", event.get("is_on_blacklist"));
			DBObject upd = new BasicDBObject("$set", set);
			
			WriteResult wr = coll.update(doc, upd);
			updated = wr.getN() > 0;
		}
		return updated;
	}
	
	private boolean blacklistFieldsHaveChanged(Map<String, Object> event) {
		boolean needsUpdate = false;
		if (canRead && event.containsKey("removed_from_blacklist") && event.containsKey("is_on_blacklist")) {
			DBObject doc = new BasicDBObject();
			doc.put("sha2_256", event.get("sha2_256"));
			doc.put("prefix", event.get("prefix"));
			doc.put("reported_at", event.get("reported_at"));
			DBObject existing = coll.findOne(doc);
			if (existing != null) {
				long existingTime = Long.valueOf(existing.get("removed_from_blacklist").toString());
				long eventTime = Long.valueOf(event.get("removed_from_blacklist").toString());
				boolean blTimeUpdated = eventTime > existingTime;
				boolean blFlagChanged =  !existing.get("is_on_blacklist").equals(event.get("is_on_blacklist"));
				needsUpdate = blTimeUpdated || blFlagChanged;  
			}
		}
		return needsUpdate;
	}
	
	/**
	 * Find the event reports currently marked as blacklisted for the reporter provided
	 * @param prefix blacklisting source's assigned prefix
	 * @param field the key to retrieve from each report
	 * @return a Set of Strings containing the value for the corresponding key
	 */
	public Set<String> findCurrentlyBlacklistedBySource(String prefix, String field) {
		DBObject query = new BasicDBObject();
		query.put("prefix", prefix.toLowerCase());
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
	
	@Override
	protected List<DBObject> getResults(DBObject searchFor) {
		return findAndSetUid(searchFor);
	}
	
}
