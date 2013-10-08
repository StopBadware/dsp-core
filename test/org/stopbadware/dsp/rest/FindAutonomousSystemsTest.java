package org.stopbadware.dsp.rest;

import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.Test;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class FindAutonomousSystemsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void getASTest() {
		String basePath = "/v2/asns/";
		HTTP.searchTest(basePath + PRIVATE_AS_RANGE_START);
		HTTP.errorTest(basePath + "NAN", Error.BAD_FORMAT);
	}

}
