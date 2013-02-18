package org.stopbadware.dsp.sec;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.subject.Subject;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {
	
	private static Realm realm = new Realm();

	public static boolean authenticated(HttpHeaders httpHeaders, String path) {
		String key = httpHeaders.getRequestHeaders().getFirst("sbw_key");
		String sig = httpHeaders.getRequestHeaders().getFirst("sbw_sig");
		long ts = Long.valueOf(httpHeaders.getRequestHeaders().getFirst("sbw_ts"));
		System.out.println(key);
		System.out.println(sig);
		System.out.println(ts);
		System.out.println(path);
		
		SecurityManager securityManager = new DefaultSecurityManager(realm);
		SecurityUtils.setSecurityManager(securityManager);
		Subject subject = SecurityUtils.getSubject();
		AuthenticationToken token = new RESTfulToken(key, sig, ts); 
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
