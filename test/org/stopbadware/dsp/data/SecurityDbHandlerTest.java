package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

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
	private static String testParticipant = TEST;
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
		String disabledTestApiKey = dbh.addUser(testRoles, testParticipant, subject);
		boolean disabled = dbh.disableUser(disabledTestApiKey, subject);
		assertTrue(disabled);
	}
	
	@Test
	public void getParticipantTest() {
		String participant = dbh.getParticipant(testApiKey);
		assertTrue(participant != null);
		assertTrue(participant.length() > 0);
		assertTrue(participant.equals(testParticipant));
	}

	@Test
	public void addUserTest() {
		String addUserTestApiKey = dbh.addUser(testRoles, testParticipant, subject);
		assertTrue(addUserTestApiKey != null); 
		assertTrue(addUserTestApiKey.length() > 0);
	}

	@Test
	public void getRolesTest() {
		Set<String> roles = dbh.getRoles(testApiKey);
		assertTrue(roles != null);
		assertTrue(roles.size() == testRoles.size());
		for (Role r : testRoles) {
			assertTrue(roles.contains(r.toString()));
		}
	}

	@Test
	public void getObjectPermissionsTest() {
		Set<Permission> perms = dbh.getObjectPermissions(testApiKey);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
	}

	@Test
	public void getStringPermissionsTest() {
		Set<String> perms = dbh.getStringPermissions(testApiKey);
		assertTrue(perms != null);
		assertTrue(perms.size() > 0);
	}

	@Test
	public void getRolePermissionsTest() {
		for (Role r : Role.values()) {
			Set<String> perms = dbh.getRolePermissions(r.toString());
			assertTrue(perms != null);
			if (r.equals(Role.NO_PERMISSIONS)) {
				assertTrue(perms.size() == 0);
			} else {
				assertTrue(perms.size() > 0);
			}
		}
	}

}
