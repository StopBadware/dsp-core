package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.io.IOException;
import java.util.Map;

import org.junit.Test;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class SecureRestTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void validAuthTest() {
		assertTrue(HTTP.getTest(VALID_API_PATH).code == OK);
	}
	
	@Test
	public void notFoundTest() {
		assertTrue(HTTP.getTest("/azeroth").code == NOT_FOUND);
	}
	
	@Test
	public void invalidPathTest() {
		Map<String, String> headers = HTTP.createAuthHeaders(VALID_API_PATH);
		assertTrue(headerTest(headers, "/v2/events/timeoflast/Bronzebeard") == FORBIDDEN);
	}
	
	@Test
	public void invalidPublicKeyTest() {
		Map<String, String> headers = modifiedHeaders("SBW-Key", "Thrall");
		assertTrue(headerTest(headers) == FORBIDDEN);
		headers = HTTP.createAuthHeaders(null, "Windrunner", null, null, null);
		assertTrue(headerTest(headers) == FORBIDDEN);
	}
	
	@Test
	public void invalidSecretKeyTest() {
		Map<String, String> headers = HTTP.createAuthHeaders(null, null, null, "Wrynn", null);
		assertTrue(headerTest(headers) == FORBIDDEN);
	}
	
	@Test
	public void invalidTimestampTest() {
		Map<String, String> headers = modifiedHeaders("SBW-Timestamp", "Bloodhoof");
		assertTrue(headerTest(headers) == FORBIDDEN);
		headers = HTTP.createAuthHeaders(null, null, "Proudmoore", null, null);
		assertTrue(headerTest(headers) == FORBIDDEN);
	}
	
	@Test
	public void invalidSigTest() {
		Map<String, String> headers = modifiedHeaders("SBW-Signature", "Voljin");
		assertTrue(headerTest(headers) == FORBIDDEN);
		headers = HTTP.createAuthHeaders(null, null, null, null, "Gallywix");
		assertTrue(headerTest(headers) == FORBIDDEN);
	}
	
	private Map<String, String> modifiedHeaders(String key, String value) {
		Map<String, String> headers = HTTP.createAuthHeaders(VALID_API_PATH);
		headers.put(key, value);
		return headers;
	}
	
	private int headerTest(Map<String, String> authHeaders) {
		return headerTest(authHeaders, VALID_API_PATH);
	}
	
	private int headerTest(Map<String, String> authHeaders, String path) {
		try {
			return HTTP.sendTestHttpRequest(path, null, "GET", authHeaders).code;
		} catch (IOException e) {
			fail("IOException thrown: " + e.getMessage());
			return 0; 
		}
	}

}
