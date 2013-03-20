package org.stopbadware.dsp.json;

import java.util.Set;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Serialization wrapper for marshaling/unmarhsaling which
 * hosts and/or IPs the Resolver should resolve 
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ResolverRequest implements Response {
	
	private Set<String> hosts;
	
	public ResolverRequest() {
		
	}
	
	public ResolverRequest(Set<String> hosts) {
		this.hosts = hosts;
	}
	
	public Set<String> getHosts() {
		return hosts;
	}

}
