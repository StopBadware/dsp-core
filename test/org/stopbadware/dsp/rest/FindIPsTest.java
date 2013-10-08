package org.stopbadware.dsp.rest;

import org.junit.Test;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class FindIPsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void findSinceTest() {
		String basePath = "/v2/ips/";
		HTTP.searchTest(basePath + "0");
		HTTP.searchTest(basePath + "0.0.0.0");
	}

}
