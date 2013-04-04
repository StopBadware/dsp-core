package org.stopbadware.dsp.json;

import org.codehaus.jackson.annotate.JsonProperty;

public class AccountInfo implements Response {
	
	@JsonProperty("api_key")
	private String apiKey;
	private String secret;
	
	public AccountInfo(String apiKey, String secret) {
		this.setApiKey(apiKey);
		this.setSecret(secret);
	}

	public String getSecret() {
		return secret;
	}

	public void setSecret(String secret) {
		this.secret = secret;
	}

	public String getApiKey() {
		return apiKey;
	}

	public void setApiKey(String apiKey) {
		this.apiKey = apiKey;
	}
	
}
