package org.stopbadware.dsp.json;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling maximum
 * number of threads the Resolver uses 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class MaxThreads {
	
	@JsonProperty("as_of")
	private long asOf;
	@JsonProperty("max_threads")
	private int max;
	
	public MaxThreads(int max) {
		this.max = max;
		asOf = System.currentTimeMillis() / 1000;
	}

	public long getAsOf() {
		return asOf;
	}

	public int getMax() {
		return max;
	}

}
