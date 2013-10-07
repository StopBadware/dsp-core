package org.stopbadware.dsp.sec;

import java.net.URI;

import javax.ws.rs.core.HttpHeaders;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.SecurityDbHandler;

/**
 * Authentication and authorization handler 
 */
public abstract class AuthAuth {
	
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	private static final long MAX_AGE = Long.valueOf(System.getenv("MAX_AUTH_AGE"));
	private static final Logger LOG = LoggerFactory.getLogger(AuthAuth.class);
	
	static {
		SecurityUtils.setSecurityManager(securityManager);
		((DefaultSessionManager)((SessionsSecurityManager) securityManager).getSessionManager()).setSessionValidationSchedulerEnabled(false);
		SessionStorageEvaluator sessionDAO = ((DefaultSubjectDAO)((DefaultSecurityManager) securityManager).getSubjectDAO()).getSessionStorageEvaluator();
		((DefaultSessionStorageEvaluator)sessionDAO).setSessionStorageEnabled(false);
	}

	/**
	 * Creates and returns a subject from the provided parameters
	 * @param httpHeaders HTTP Header information that should include
	 * "SBW-Key", "SBW-Signature", and "SBW-Timestamp" - otherwise
	 * a warning will be logged and null returned
	 * @param uri destination URI of the request
	 * @return an authenticated Subject for use in authorization and 
	 * authentication checks, or null if authentication failed
	 */
	public static Subject getSubject(HttpHeaders httpHeaders, URI uri) {
		String path = null;
		String key = null;
		String sig = null;
		long ts = 0L;
		try {
			path = uri.getPath().toString();
			key = httpHeaders.getRequestHeaders().getFirst("SBW-Key");
			sig = httpHeaders.getRequestHeaders().getFirst("SBW-Signature");
			ts = Long.valueOf(httpHeaders.getRequestHeaders().getFirst("SBW-Timestamp"));
			LOG.info("{} accessing '{}'", key, path);
		} catch (NullPointerException | IllegalStateException | NumberFormatException e) {
			LOG.warn("Exception thrown parsing headers:\t{}", e.toString());
		}
		
		Subject subject = SecurityUtils.getSubject();
		subject.logout();
		if (keyIsValid(key) && sigIsValid(sig) && tsIsValid(ts)) {
			RestToken token = new RestToken(key, sig, path, ts);
			try {
				subject.login(token);
			} catch (AuthenticationException e) {
				LOG.warn("Authentication failure for API Key {}:\t{}", token.getPrincipal(), e.getMessage());
			} 
		}
		
		return (subject.isAuthenticated()) ? subject : null;
	}
	
	private static boolean keyIsValid(String key) {
		return (key != null && key.length() > 0);
	}
	
	private static boolean sigIsValid(String sig) {
		return (sig != null && sig.length() > 0);
	}
	
	private static boolean tsIsValid(long ts) {
		long age = (System.currentTimeMillis()/1000) - ts;
		return age < MAX_AGE;
	}
	
	/**
	 * Checks if the provided subject is associated with a specific participant
	 * @param subject the Shiro Subject to check
	 * @param participant case insensitive prefix of the participant
	 * @return true if, and only if, the account is associated with the participant
	 */
	public static boolean subjectIsMemberOf(Subject subject, String participant) {
		SecurityDbHandler db = new SecurityDbHandler();
		return db.getParticipant(subject.getPrincipal().toString()).equalsIgnoreCase(participant);
	}
	
}
