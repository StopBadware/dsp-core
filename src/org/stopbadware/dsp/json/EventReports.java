package org.stopbadware.dsp.json;

import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Serialization wrapper for marshalling/unmarhsalling timestamp 
 * of the most recent event report for a source 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventReports {
	
	private int size;
	private long time;
	private Set<Map<String, Object>> reports;
	
	public EventReports() {
		reports = null;
		size = 0;
		time = 0;
	}
	
	public EventReports(Set<Map<String, Object>> reports) {
		this.reports = reports;
		size = reports.size();
		time = System.currentTimeMillis() / 1000;
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
}
