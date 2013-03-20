package org.stopbadware.dsp.json;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling individual event reports
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ERWrapper implements Response {

	private String host;
	@JsonProperty("share_level")
	private String shareLevel;
	@JsonProperty("er_map")
	private Map<String, Object> erMap;
	
	public ERWrapper() {
		host = null;
		shareLevel = null;
		erMap = null;
	}
	
	public ERWrapper(String host, String shareLevel, Map<String, Object> erMap) {
		this.host = host;
		this.shareLevel = shareLevel;
		this.erMap = erMap;
	}

	public String getHost() {
		return host;
	}

	public String getShareLevel() {
		return shareLevel;
	}

	public Map<String, Object> getErMap() {
		return erMap;
	}
	
}
