package org.stopbadware.dsp.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Serialization wrapper for marshaling/unmarhsaling timestamp 
 * of the most recent event report for a source 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TimeOfLast implements Response {
	
	private long last;
	private String source;
	
	public TimeOfLast() {
		last = 0;
		setSource(null);
	}
	
	public TimeOfLast(String source, long last) {
		this.source = source;
		this.last = last;
	}

	public long getLast() {
		return last;
	}

	public void setLast(long last) {
		this.last = last;
	}

	public String getSource() {
		return source;
	}

	public void setSource(String source) {
		this.source = source;
	}

}
