package org.stopbadware.dsp;

import org.stopbadware.dsp.data.SecurityDbHandler;

public class RateLimitException extends Exception {
	
	private static final String jsonHead = "{\"code\":429,\"rate_limit_reset_time\":";
	private static final String jsonTail = ",\"error\":\"rate limit exceeded\"}";
	private static final long serialVersionUID = 4064234295281708394L;
	
	public RateLimitException() {
		super(jsonHead+((System.currentTimeMillis()/1000)+SecurityDbHandler.RATE_LIMIT_SECS)+jsonTail);
	}
	
}
