package org.stopbadware.dsp;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {

	public static boolean authenticated(HttpHeaders httpHeaders) {
		String key = httpHeaders.getRequestHeaders().getFirst("sbw_key");
		String sig = httpHeaders.getRequestHeaders().getFirst("sbw_sig");
		String ts = httpHeaders.getRequestHeaders().getFirst("sbw_ts");
		System.out.println(key);
		System.out.println(ts);
		System.out.println(sig);
		Subject subject = SecurityUtils.getSubject();
		System.out.println(subject.isAuthenticated());
		return false;
	}
}
