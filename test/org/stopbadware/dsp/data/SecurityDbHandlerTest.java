package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.Test;
import org.stopbadware.dsp.sec.RestToken;
import org.stopbadware.dsp.sec.Realm;
import org.stopbadware.dsp.sec.Role;

public class SecurityDbHandlerTest {
	
	private static Subject subject = null;
	private static SecurityDbHandler dbh = null; 
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	private static final String TEST_APIKEY = "DATA123456";
	
	static {
		realm.setCachingEnabled(false);
		SecurityUtils.setSecurityManager(securityManager);
		subject = SecurityUtils.getSubject();
		String sig = "54fc7ffd3cdc856c09c8747b61718741166f347b93f43c8db2ce6e4f568881e1";
		String path = "/v0.2/events/since/0";
		long ts = 1294513200L;
		
		RestToken token = new RestToken(TEST_APIKEY, sig, path, ts); 
		try {
			subject.login(token);
		} catch (AuthenticationException e) {
			fail("AuthenticationException thrown");
		}
		dbh = new SecurityDbHandler();
	}

	@Test
	public void testGetSecret() {
		String secret = dbh.getSecret(subject.getPrincipal().toString());
		assertTrue(secret != null);
		assertTrue(secret.length() > 0);
	}

	@Test
	public void testAddUser() {
		Set<Role> roles = new HashSet<>();
		String newAPIKey = dbh.addUser(roles, subject, "test");
		assertTrue(newAPIKey != null); 
		assertTrue(newAPIKey.length() > 0);
	}

	@Test
	public void testGetRoles() {
		Set<String> roles = dbh.getRoles(TEST_APIKEY);
		assertTrue(roles != null);
		assertTrue(roles.size() > 0);
	}

	@Test
	public void testGetObjectPermissions() {
		Set<Permission> perms = dbh.getObjectPermissions(TEST_APIKEY);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
	}

	@Test
	public void testGetStringPermissions() {
		Set<String> perms = dbh.getStringPermissions(TEST_APIKEY);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
	}

	@Test
	public void testGetRolePermissions() {
		Set<String> perms = dbh.getRolePermissions(Role.ADMIN.toString());
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
	}

}