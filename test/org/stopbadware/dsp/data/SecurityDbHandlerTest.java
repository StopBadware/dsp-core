package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.sec.Role;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;

public class SecurityDbHandlerTest {
	
	private static Subject subject = AuthAuthTestHelper.getSubject();
	private static SecurityDbHandler dbh = new SecurityDbHandler(); 
	private static final String TEST_APIKEY = "TEST_APIKEY";
//	private static final String TEST_SECRET = "TEST_SECRET";
	
	@BeforeClass
	public static void addTestAccount() {
		//TODO DATA-120
	}
	
	@Test
	public void getSecretTest() {
		String secret = dbh.getSecret(subject.getPrincipal().toString());
		assertTrue(secret != null);
		assertTrue(secret.length() > 0);
		assertTrue(false);
	}
	
	@Test
	public void disableUserTest() {
		assertTrue(false);
	}
	
	@Test
	public void getParticipantTest() {
		assertTrue(false);
	}

	@Test
	public void addUserTest() {
//		Set<Role> roles = new HashSet<>();
//		String newAPIKey = dbh.addUser(roles, subject, "test");
//		assertTrue(newAPIKey != null); 
//		assertTrue(newAPIKey.length() > 0);
		assertTrue(false);
	}

	@Test
	public void getRolesTest() {
		Set<String> roles = dbh.getRoles(TEST_APIKEY);
		assertTrue(roles != null);
		assertTrue(roles.size() > 0);
		assertTrue(false);
	}

	@Test
	public void getObjectPermissionsTest() {
		Set<Permission> perms = dbh.getObjectPermissions(TEST_APIKEY);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
		assertTrue(false);
	}

	@Test
	public void getStringPermissionsTest() {
		Set<String> perms = dbh.getStringPermissions(TEST_APIKEY);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
		assertTrue(false);
	}

	@Test
	public void getRolePermissionsTest() {
		Set<String> perms = dbh.getRolePermissions(Role.ADMIN.toString());
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
		assertTrue(false);
	}

}
