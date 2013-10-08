package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper.TestResponse;

public class FindAutonomousSystemsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void getASTest() {
		String path = "/v2/asns/" + PRIVATE_AS_RANGE_START;
		TestResponse res = HTTP.getTest(path);
		assertTrue(res != null);
		assertTrue(HTTP.getTest(path).code == OK);
		ObjectMapper mapper = new ObjectMapper();
		try {
			SearchResults results = mapper.readValue(res.body, SearchResults.class);
			assertTrue(results.getCode() == SearchResults.OK);
		} catch (IOException e) {
			fail("IOException thrown: "+e.getMessage());
		}
		
	}

}
