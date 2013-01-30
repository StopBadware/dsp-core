package org.stopbadware.dsp.json;

import java.util.Map;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * Serialization wrapper for marshaling/unmarhsaling data
 * generated by the Resolver
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolverResults {
	
	private long time;
	@JsonProperty("host_to_ip_size")
	private int hostToIPSize;
	@JsonProperty("ip_to_as_size")
	private int ipToASSize;
	@JsonProperty("host_to_ip")
	private Map<String, Long> hostToIPMappings;
	@JsonProperty("ip_to_as")
	private Map<Long, Map<String, Object>> ipToASMappings;
	
	public ResolverResults() {
		time = System.currentTimeMillis() / 1000;
		hostToIPMappings = null;
		ipToASMappings = null;
		hostToIPSize = 0;
		ipToASSize = 0;
	}
	
	public ResolverResults(Map<String, Long> hostToIPMappings, Map<Long, Map<String, Object>> ipToASMappings) {
		time = System.currentTimeMillis() / 1000;
		this.hostToIPMappings = hostToIPMappings;
		this.ipToASMappings = ipToASMappings;
		hostToIPSize = hostToIPMappings.size();
		ipToASSize = ipToASMappings.size();
	}
	
	public long getTime() {
		return time;
	}

	public Map<String, Long> getHostToIPMappings() {
		return hostToIPMappings;
	}

	public Map<Long, Map<String, Object>> getIpToASMappings() {
		return ipToASMappings;
	}

	public int getHostToIPSize() {
		return hostToIPSize;
	}

	public int getIpToASSize() {
		return ipToASSize;
	}
	
}
