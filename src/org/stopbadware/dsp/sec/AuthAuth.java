package org.stopbadware.dsp.sec;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.lib.util.SHA2;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {
	
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	private static final Logger LOG = LoggerFactory.getLogger(AuthAuth.class);
	public static String REALMNAME = "SBW-DSP";

	public static boolean authenticated(HttpHeaders httpHeaders, String path) {
		String key = httpHeaders.getRequestHeaders().getFirst("SBW-Key");
		String sig = httpHeaders.getRequestHeaders().getFirst("SBW-Signature");
		long ts = Long.valueOf(httpHeaders.getRequestHeaders().getFirst("SBW-Timestamp"));
		
		SecurityUtils.setSecurityManager(securityManager);
		Subject subject = SecurityUtils.getSubject();
		AuthenticationToken token = new RESTfulToken(key, sig, path, ts); 
		boolean authenticated = false;
		
		try {
			subject.login(token);
			System.out.println(subject.toString());
			System.out.println(subject.isAuthenticated());
			authenticated = subject.isAuthenticated();
		} catch (AuthenticationException e) {
			System.err.println(e.getMessage());	//DELME: DATA-54
			LOG.warn("Authentication failure for API Key {}:\t{}", e.getMessage());
		}
		
		return authenticated;
	}
}
