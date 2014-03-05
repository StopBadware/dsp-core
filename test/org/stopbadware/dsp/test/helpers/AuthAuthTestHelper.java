package org.stopbadware.dsp.test.helpers;

import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.mgt.SessionStorageEvaluator;
import org.apache.shiro.mgt.SessionsSecurityManager;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.apache.shiro.subject.Subject;
import org.stopbadware.dsp.sec.Realm;
import org.stopbadware.dsp.sec.RestToken;
import org.stopbadware.lib.util.SHA2;

/**
 * Helper class to provide an authenticated subject for testing purposes. 
 *
 */
public class AuthAuthTestHelper {
	
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	
	static {
		SecurityUtils.setSecurityManager(securityManager);
		((DefaultSessionManager)((SessionsSecurityManager) securityManager).getSessionManager()).setSessionValidationSchedulerEnabled(false);
		SessionStorageEvaluator sessionDAO = ((DefaultSubjectDAO)((DefaultSecurityManager) securityManager).getSubjectDAO()).getSessionStorageEvaluator();
		((DefaultSessionStorageEvaluator)sessionDAO).setSessionStorageEnabled(false);
	}
	
	public static Subject getSubject() {
		Subject subject = SecurityUtils.getSubject();
		long ts = 0L;
		String sig = SHA2.get256(TEST+ts+TEST+TEST);
		RestToken token = new RestToken(TEST, sig, TEST, ts);
		subject.login(token);
		return subject;
	}
	
	public static Subject getRateLimitSubject() {
		String rlTest = "RATELIMITTEST";
		Subject subject = SecurityUtils.getSubject();
		long ts = 0L;
		String sig = SHA2.get256(rlTest+ts+rlTest+rlTest);
		RestToken token = new RestToken(rlTest, sig, rlTest, ts);
		subject.login(token);
		return subject;
	}

}
