package org.stopbadware.dsp.json;

import java.util.Collection;
import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling search results 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResults implements Response {
	
	private int count = 0;
	private int duration = 0;
	private long startedAt = 0L;
	private long completedAt = 0L;
	private String searchCriteria = "";
	private Object results = null;
	
	public SearchResults(String searchCriteria) {
		startedAt = System.currentTimeMillis();
		this.searchCriteria = searchCriteria;
	}
	
	public int getCount() {
		return count;
	}
	
	@JsonProperty("completed_at")
	public long getCompletedAt() {
		return completedAt / 1000L;
	}

	private void setCompletedAt(long completedAt) {
		this.completedAt = completedAt;
	}

	@JsonProperty("search_criteria")
	public String getSearchCriteria() {
		return searchCriteria;
	}
	
	@JsonProperty("search_results")
	public Object getResults() {
		return results;
	}
	
	public void setResults(Collection<?> results) {
		this.results = results;
		count = results.size();
		setDuration();
	}

	public void setResults(Map<?, ?> results) {
		this.results = results;
		count = results.size();
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
