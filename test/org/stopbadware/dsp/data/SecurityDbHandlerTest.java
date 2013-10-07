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
	private static String testApiKey = null;
	private static String testSecret = null;
	private static String testParticipant = "test";
	private static Set<Role> testRoles =  new HashSet<>();
	
	@BeforeClass
	public static void addTestAccount() {
		testRoles.add(Role.PUBLIC);
		testApiKey = dbh.addUser(testRoles, testParticipant, subject);
		testSecret = dbh.getSecret(testApiKey);
		assertTrue(testApiKey != null);
	}
	
	@Test
	public void getSecretTest() {
		String secret = dbh.getSecret(testApiKey);
		assertTrue(secret != null);
		assertTrue(secret.length() > 0);
		assertTrue(secret.equals(testSecret));
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
//		assertTrue(newAPIKey != null); 
//		assertTrue(newAPIKey.length() > 0);
		assertTrue(false);
	}

	@Test
	public void getRolesTest() {
		Set<String> roles = dbh.getRoles(testApiKey);
		assertTrue(roles != null);
		assertTrue(roles.size() > 0);
		assertTrue(false);
	}

	@Test
	public void getObjectPermissionsTest() {
		Set<Permission> perms = dbh.getObjectPermissions(testApiKey);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
		assertTrue(false);
	}

	@Test
	public void getStringPermissionsTest() {
		Set<String> perms = dbh.getStringPermissions(testApiKey);
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
