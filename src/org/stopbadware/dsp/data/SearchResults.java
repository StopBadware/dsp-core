package org.stopbadware.dsp.data;

public class SearchResults {
	
	private int count = 0;
	private long completed_at = 0L;
	private String search_criteria = "";
	private String results = "";
	
	public SearchResults() {
		completed_at = System.currentTimeMillis();
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getCompleted_at() {
		return completed_at;
	}

	public void setCompleted_at(long completed_at) {
		this.completed_at = completed_at;
	}

	public String getSearch_criteria() {
		return search_criteria;
	}

	public void setSearch_criteria(String search_criteria) {
		this.search_criteria = search_criteria;
	}

	public String getResults() {
		return results;
	}

	public void setResults(String results) {
		this.results = results;
	}
}
