package org.stopbadware.dsp.json;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper container for marshaling/unmarhsaling event reports
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventReports implements Response {
	
	private int size;
	private long time;
	private String source;
	@JsonProperty("is_differential")
	private boolean isDifferentialBlacklist;
	private Set<ERWrapper> reports;
	
	public EventReports() {
		reports = null;
		size = 0;
		time = 0;
		source = null;
		isDifferentialBlacklist = false;
	}
	
	public EventReports(String source, Set<ERWrapper> reports) {
		this.reports = reports;
		size = reports.size();
		time = System.currentTimeMillis() / 1000;
		this.source = source;
		isDifferentialBlacklist = false;
	}
	
	public EventReports(String source, Set<ERWrapper> reports, boolean isDifferentialBlacklist) {
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

	public Set<ERWrapper> getReports() {
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
