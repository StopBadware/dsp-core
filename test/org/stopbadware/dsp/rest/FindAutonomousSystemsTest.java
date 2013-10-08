package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Test;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class FindAutonomousSystemsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void getASTest() {
		String path = "/v2/asns/" + PRIVATE_AS_RANGE_START;
		//TODO DATA-120 test response is SearchResults
		ObjectMapper mapper = new ObjectMapper();
		assertTrue(HTTP.getTest(path).code == OK);
	}

}
