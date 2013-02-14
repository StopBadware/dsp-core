package org.stopbadware.dsp.sec;

import java.util.HashMap;
import java.util.Map;

import org.apache.shiro.authc.AuthenticationToken;

public class RESTfulToken implements AuthenticationToken {

	private String apiKey;
	private Map<String, Object> credentials;
	private static final long serialVersionUID = -5394243945291266097L;

	public RESTfulToken(String apiKey, String signature, long timestamp) {
		credentials = new HashMap<>();
		credentials.put("signature", signature);
		credentials.put("timestamp", timestamp);
		this.apiKey = apiKey;
	}
	
	@Override
	public Map<String, Object> getCredentials() {
		return credentials;
	}

	@Override
	public String getPrincipal() {
		return apiKey;
	}

}
