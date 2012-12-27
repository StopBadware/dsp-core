package org.stopbadware.dsp.data;

public class SearchResults {
	
	private int count = 0;
	private long duration = 0L;
	private long started_at = 0L;
	private long completed_at = 0L;
	private String search_criteria = "";
	private Object results = new Object();
	
	public SearchResults() {
		started_at = System.currentTimeMillis();
	}
	
	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public long getCompleted_at() {
		return completed_at/1000;
	}

	private void setCompleted_at(long completed_at) {
		this.completed_at = completed_at;
	}

	public String getSearch_criteria() {
		return search_criteria;
	}

	public void setSearch_criteria(String search_criteria) {
		this.search_criteria = search_criteria;
	}

	public Object getResults() {
		return results;
	}

	public void setResults(Object results) {
		this.results = results;
		setDuration();
	}

	public long getDuration() {
		return duration;
	}

	public void setDuration() {
		setCompleted_at(System.currentTimeMillis());
		this.duration = completed_at - started_at;
	}
}
