package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.util.HashMap;
import java.util.HashSet;

import org.junit.Test;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.CleanReports;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.EventReports;
import org.stopbadware.dsp.json.ResolverResults;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class AddTest {
	
	private static final String PREFIX = "test";
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void addEventsTest() {
		String path = "/v2/add/events";
		EventReports data = new EventReports(PREFIX, new HashSet<ERWrapper>());
		assertTrue(HTTP.postTest(path, data, OK));
	}
	
	@Test
	public void pushToApiTest() {
		String path = "/v2/add/" + PREFIX;
		String data = "{\"share_level\": \"PUB\",\"is_diff\": false,\"reports\": []}";
		/* Pushing reports to Importer expects a whitelisted source (prefix), thus
		 * testing for 400 instead of 200 to avoid either using an actual prefix 
		 * for testing or adding special handling for tests */
		assertTrue(HTTP.postTest(path, data, BAD_REQUEST));
	}
	
	@Test
	public void pushToApiTestNotFound() {
		String path = "/v2/add/" + "nosuchprefix";
		String data = "{\"share_level\": \"PUB\",\"is_diff\": false,\"reports\": []}";
		assertTrue(HTTP.postTest(path, data, NOT_FOUND));
	}
	
	@Test
	public void markCleanTest() {
		String path = "/v2/add/clean";
		assertTrue(HTTP.postTest(path, new CleanReports(), OK));
	}
	
	@Test
	public void addResolvedTest() {
		String path = "/v2/add/resolved";
		ResolverResults data = new ResolverResults(new HashMap<String, Long>(), new HashMap<Long, AutonomousSystem>());
		assertTrue(HTTP.postTest(path, data, OK));
	}
	
}
