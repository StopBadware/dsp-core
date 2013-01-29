package org.stopbadware.dsp.json;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Serialization wrapper for marshaling/unmarhsaling clean reports 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class CleanReports {
	
	private int size;
	private long time;
	private String source;
	private Set<String> clean;
	
	public CleanReports() {
		clean = null;
		source = "";
		size = 0;
		time = 0;
	}
	
	public CleanReports(String source, Set<String> clean) {
		this.clean = clean;
		this.source = source;
		size = clean.size();
		time = System.currentTimeMillis() / 1000;
	}

	public int getSize() {
		return size;
	}

	public long getTime() {
		return time;
	}
	
	public String getSource() {
		return source;
	}

	public Set<String> getClean() {
		return clean;
	}

}
