package org.stopbadware.dsp.rest;

import org.junit.Test;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class FindHostsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void findSinceTest() {
		String path = "/v2/hosts/blacklisted/now";
		HTTP.searchTest(path);
	}
	
	@Test
	public void getHostTest() {
		String path = "/v2/hosts/example.com";
		HTTP.searchTest(path);
	}

}
