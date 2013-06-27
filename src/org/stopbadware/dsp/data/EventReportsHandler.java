package org.stopbadware.dsp.data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.sec.Permissions;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBObject;

public class EventReportsHandler extends MDBCollectionHandler {
	
	public EventReportsHandler(DB db, DBCollection coll, boolean canRead, boolean canWrite) {
		super(db, coll, canRead, canWrite);
	}
	
	/**
	 * Finds Event Reports since the specified timestamp, up to a maximum of 25K
	 * sorted by reported at time ascending
	 * @param sinceTime UNIX timestamp to retrieve reports since
	 * @return SearchResults with the results or null if not authorized
	 */
	public SearchResults findEventReportsSince(long sinceTime) {
		SearchResults sr = null;
		if (sinceTime > 0) {
			sr = new SearchResults(String.valueOf(sinceTime));
			DBObject query = new BasicDBObject("reported_at", new BasicDBObject(new BasicDBObject("$gte", sinceTime)));
			DBObject keys = new BasicDBObject("_id", 0);
			keys.put("_created", 0);
			DBObject sort = new BasicDBObject("reported_at", ASC);
			int limit = 25000;
			List<DBObject> res = coll.find(query, keys).sort(sort).limit(limit).toArray();
			sr.setCount(res.size());
			sr.setResults(res);
		}
		return sr;
	}
	
	public SearchResults getEventReportsStats(String source) {
		SearchResults sr = null;
		if (canRead) {
			sr = new SearchResults(source);
			Map<String, Object> stats = new HashMap<>();
			stats.put("total_count", coll.getCount());
			stats.put("on_blacklist_count", coll.getCount(new BasicDBObject("is_on_blacklist", true)));
			//TODO: DATA-96 get added between start & end
			stats.put("added_last24h", getEventReportsAddedBetween(0, 0));
			stats.put("added_last7d", getEventReportsAddedBetween(0, 0));
			stats.put("added_last4w", getEventReportsAddedBetween(0, 0));
			sr.setCount(stats.size());
			sr.setResults(stats);
		}
		return sr;
	}
	
	private int getEventReportsAddedBetween(long start, long end) {
		//TODO: DATA-96 get added between start & end
		if (canRead) {
			
		}
		return 0;
	}	
	
	
}
