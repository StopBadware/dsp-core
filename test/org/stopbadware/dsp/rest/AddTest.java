package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.HashSet;

import org.junit.Test;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.EventReports;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class AddTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void addEventsTest() {
		String path = "/v2/add/events";
		EventReports data = new EventReports("test", new HashSet<ERWrapper>());
		try {
			int response = HTTP.sendTestHttpRequest(path, data);
			assertTrue(response == 200);
		} catch (IOException e) {
			fail("IOException thrown: "+e.getMessage());
		}
	}
	
}
