package org.stopbadware.dsp.sec;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;
import org.stopbadware.lib.util.SHA2;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {
	
	private static Realm realm = new Realm();
	public static String REALMNAME = "SBW-DSP";

	public static boolean authenticated(HttpHeaders httpHeaders, String path) {
		String key = httpHeaders.getRequestHeaders().getFirst("SBW-Key");
		String sig = httpHeaders.getRequestHeaders().getFirst("SBW-Signature");
		long ts = Long.valueOf(httpHeaders.getRequestHeaders().getFirst("SBW-Timestamp"));
		System.out.println(key);
		System.out.println(sig);
		System.out.println(ts);
		System.out.println(path);
		System.out.println(SHA2.get256(key+ts+path+"SECRET"));
		SecurityManager securityManager = new DefaultSecurityManager(realm);
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
			System.err.println(e.getMessage());
		}
		return authenticated;
	}
}
