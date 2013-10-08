package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.HttpRequestTestHelper.*;

import java.util.HashSet;

import org.junit.Test;
import org.stopbadware.dsp.data.SecurityDbHandler;
import org.stopbadware.dsp.sec.Role;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class AdminTest {
	
	private static final String PREFIX = "test";
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void createNewAccountTest() {
		String path = "/v2/admin/account/create/" + PREFIX;
		//TODO DATA-120 test response is AccountInfo
		assertTrue(HTTP.sendTest(path, null, OK));
	}
	
	@Test
	public void disableAccountTest() {
		String apiKey = new SecurityDbHandler().addUser(new HashSet<Role>(), PREFIX, AuthAuthTestHelper.getSubject());
		String path = "/v2/admin/account/disable/" + apiKey;
		assertTrue(HTTP.sendTest(path, null, OK));
	}

}