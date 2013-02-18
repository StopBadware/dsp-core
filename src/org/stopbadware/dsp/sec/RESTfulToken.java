package org.stopbadware.dsp.sec;

import org.apache.shiro.authc.AuthenticationToken;

public class RESTfulToken implements AuthenticationToken {

	private String apiKey;
	private Credentials credentials;
	private static final long serialVersionUID = -5394243945291266097L;

	public RESTfulToken(String apiKey, String signature, long timestamp) {
		credentials = new Credentials(signature, timestamp);
		this.apiKey = apiKey;
	}
	
	@Override
	public Credentials getCredentials() {
		return credentials;
	}

	@Override
	public String getPrincipal() {
		return apiKey;
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj instanceof RESTfulToken) {
			return this.apiKey.equals(((RESTfulToken) obj).getPrincipal());
		} else {
			return false;
		}
	}
	
	@Override
	public int hashCode() {
		return apiKey.hashCode();
	}
	
	static public class Credentials {
		
		private String signature = "";
		private long timestamp = 0L;
		
		public Credentials (String signature, long timestamp) {
			this.signature = signature;
			this.timestamp = timestamp;
		}
		
		public String getSignature() {
			return signature;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Credentials) {
				return (this.signature.equals(((Credentials) obj).getSignature()) && 
						 this.timestamp == ((Credentials) obj).getTimestamp());
			} else {
				return false;
			}
		}
		
		@Override
		public int hashCode() {
			return signature.hashCode();
		}
	}

}
