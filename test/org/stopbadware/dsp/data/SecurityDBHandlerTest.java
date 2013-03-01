package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.Before;
import org.junit.Test;
import org.stopbadware.dsp.sec.RESTfulToken;
import org.stopbadware.dsp.sec.Realm;

public class SecurityDBHandlerTest {
	
	private static DBHandler dbh = null; 
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	
	static {
		SecurityUtils.setSecurityManager(securityManager);
		Subject subject = SecurityUtils.getSubject();
		String key = "DATA123456";
		String sig = "54fc7ffd3cdc856c09c8747b61718741166f347b93f43c8db2ce6e4f568881e1";
		String path = "/clearinghouse/events/test";
		long ts = 1294513200L;
		
		RESTfulToken token = new RESTfulToken(key, sig, path, ts); 
		try {
			subject.login(token);
		} catch (AuthenticationException e) {
			
			fail("AuthenticationException thrown");
		}
		MongoDB.switchToTestDB();
		dbh = new DBHandler(subject);
	}

	@Before
	public void setUp() throws Exception {
	}

	@Test
	public void testSecurityDBHandler() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetSecret() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddUser() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRoles() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetObjectPermissions() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetStringPermissions() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetRolePermissions() {
		fail("Not yet implemented");
	}

}
