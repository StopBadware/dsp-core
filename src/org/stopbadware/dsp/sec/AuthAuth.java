package org.stopbadware.dsp.sec;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {
	
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	private static final long MAX_AGE = 15L;
	private static final Logger LOG = LoggerFactory.getLogger(AuthAuth.class);
	public static String REALMNAME = "SBW-DSP";
	
	static {
		System.out.println("doin' stuff");
		SecurityUtils.setSecurityManager(securityManager);
	}

	public static boolean authenticated(HttpHeaders httpHeaders, String path) {
		String key = "";
		String sig = "";
		long ts = 0L;
		boolean authenticated = false;
		
		try {
			key = httpHeaders.getRequestHeaders().getFirst("SBW-Key");
			sig = httpHeaders.getRequestHeaders().getFirst("SBW-Signature");
			ts = Long.valueOf(httpHeaders.getRequestHeaders().getFirst("SBW-Timestamp"));
		} catch (IllegalStateException | NumberFormatException e) {
			LOG.warn("Exception thrown parsing headers:\t{}", e.getMessage());
		}
		
		if (sigIsValid(sig) && tsIsValid(ts)) {
			RESTfulToken token = new RESTfulToken(key, sig, path, ts); 
			authenticated = authenticate(token);
		}
		
		return authenticated;
	}
	
	private static boolean authenticate(RESTfulToken token) {
		boolean authenticated = false;
		Subject subject = SecurityUtils.getSubject();
		try {
			subject.login(token);
			authenticated = subject.isAuthenticated();
		} catch (AuthenticationException e) {
			LOG.warn("Authentication failure for API Key {}:\t{}", token.getPrincipal(), e.getMessage());
		}
		return authenticated;
	}
	
	private static boolean sigIsValid(String sig) {
		return (sig != null && sig.length() > 0);
	}
	
	private static boolean tsIsValid(long ts) {
		long age = (System.currentTimeMillis()/1000) - ts;
//		return age < MAX_AGE;
		return true;	//TODO: DATA-54 revert
	}
}
