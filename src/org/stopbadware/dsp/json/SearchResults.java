package org.stopbadware.dsp.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling search results 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResults {
	
	private int count = 0;
	private int duration = 0;
	private long startedAt = 0L;
	private long completedAt = 0L;
	private String searchCriteria = "";
	private Object results = null;
	
	public SearchResults() {
		startedAt = System.currentTimeMillis();
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
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

	public void setSearchCriteria(String searchCriteria) {
		this.searchCriteria = searchCriteria;
	}
	
	@JsonProperty("search_results")
	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
		setDuration();
	}

	public int getDuration() {
		return duration;
	}

	public void setDuration() {
		setCompletedAt(System.currentTimeMillis());
		this.duration = (int) (completedAt - startedAt);
	}
}
