package org.stopbadware.dsp.sec;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.ShareLevel;

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
		SecurityUtils.setSecurityManager(securityManager);
	}

	public static Subject authenticated(HttpHeaders httpHeaders, URI uri) {
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
		
		Subject subject = SecurityUtils.getSubject();
		if (sigIsValid(sig) && tsIsValid(ts)) {
			String foo = uri.getPath().toString();
			RESTfulToken token = new RESTfulToken(key, sig, foo, ts); 
//			authenticated = authenticate(token);
//			Subject subject = SecurityUtils.getSubject();
			try {
				subject.login(token);
//				authenticated = subject.isAuthenticated();
			} catch (AuthenticationException e) {
				LOG.warn("Authentication failure for API Key {}:\t{}", token.getPrincipal(), e.getMessage());
			}
		}
		
		return subject;
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
	
	public ShareLevel getAuthLevel(String apiKey) {
		return	ShareLevel.DSP_ONLY;	//TODO: DATA-54 implement 
	}
}
