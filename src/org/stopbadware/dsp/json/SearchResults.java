package org.stopbadware.dsp.json;

import java.util.Collection;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling search results 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResults implements Response {
	
	public static final int OK = 20;
	private int code = OK;
	private int count = 0;
	private int duration = 0;
	private long startedAt = 0L;
	private long completedAt = 0L;
	private Object results = null;
	
	public SearchResults() {
		startedAt = System.currentTimeMillis();
		completedAt = System.currentTimeMillis();
	}
	
	public int getCount() {
		return count;
	}
	
	public int getCode() {
		return code;
	}

	public void setCode(int code) {
		this.code = code;
	}
	
	@JsonProperty("completed_at")
	public long getCompletedAt() {
		return completedAt / 1000L;
	}

	private void setCompletedAt(long completedAt) {
		this.completedAt = completedAt;
	}
	
	@JsonProperty("search_results")
	public Object getResults() {
		return results;
	}
	
	public void setResults(Collection<?> results) {
		this.results = results;
		count = (results != null) ? results.size() : 0;
		setDuration();
	}

	public int getDuration() {
		return duration;
	}

	private void setDuration() {
		setCompletedAt(System.currentTimeMillis());
		this.duration = (int) (completedAt - startedAt);
	}
}
