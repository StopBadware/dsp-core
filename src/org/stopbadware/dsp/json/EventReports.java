package org.stopbadware.dsp.json;

import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Serialization wrapper for marshalling/unmarhsalling event reports
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventReports {
	
	private int size;
	private long time;
	private String source;
	private boolean isDifferentialBlacklist;
	private Set<Map<String, Object>> reports;
	
	public EventReports() {
		reports = null;
		size = 0;
		time = 0;
		source = null;
		isDifferentialBlacklist = false;
	}
	
	public EventReports(String source, Set<Map<String, Object>> reports) {
		this.reports = reports;
		size = reports.size();
		time = System.currentTimeMillis() / 1000;
		this.source = source;
		isDifferentialBlacklist = false;
	}
	
	public EventReports(String source, Set<Map<String, Object>> reports, boolean isDifferentialBlacklist) {
		this.reports = reports;
		size = reports.size();
		time = System.currentTimeMillis() / 1000;
		this.source = source;
		this.isDifferentialBlacklist = isDifferentialBlacklist;
	}

	public int getSize() {
		return size;
	}

	public long getTime() {
		return time;
	}

	public Set<Map<String, Object>> getReports() {
		return reports;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

	public boolean isDifferentialBlacklist() {
		return isDifferentialBlacklist;
	}

	public void setDifferentialBlacklist(boolean isDifferentialBlacklist) {
		this.isDifferentialBlacklist = isDifferentialBlacklist;
	}
}
