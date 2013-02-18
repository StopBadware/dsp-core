package org.stopbadware.dsp.sec;

import org.apache.shiro.authc.AuthenticationToken;

public class RESTfulToken implements AuthenticationToken {

	private String apiKey;
	private Credentials credentials;
	private static final long serialVersionUID = -5394243945291266097L;

	public RESTfulToken(String apiKey, String signature, String path, long timestamp) {
		credentials = new Credentials(signature, path, timestamp);
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
		private String path = "";
		private long timestamp = 0L;
		
		public Credentials (String signature, String path, long timestamp) {
			this.signature = (signature != null) ? signature : "";
			this.path = (path != null) ? path : "";
			this.timestamp = (timestamp > 0) ? timestamp : 0L;
		}
		
		public String getSignature() {
			return signature;
		}
		
		public String getPath() {
			return path;
		}
		
		public long getTimestamp() {
			return timestamp;
		}
		
		@Override
		public boolean equals(Object obj) {
			if (obj instanceof Credentials) {
				return this.signature.equals(((Credentials) obj).getSignature());
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
