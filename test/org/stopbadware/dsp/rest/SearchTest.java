package org.stopbadware.dsp.rest;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.Test;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper.TestResponse;

public class SearchTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();
	
	@Test
	public void searchEventsTest() {
		String basePath = "/v2/search/events";
		
		HTTP.searchTest(basePath + "?url=example.com");
		HTTP.searchTest(basePath + "?host=example.com");
		HTTP.searchTest(basePath + "?scheme=https");
		HTTP.searchTest(basePath + "?path=%2Fgeordi%2Flaforge");
		HTTP.searchTest(basePath + "?query=space%3Dfinalfrontier");
		HTTP.searchTest(basePath + "?reportedby=test");
		HTTP.searchTest(basePath + "?reptype=BLACKLISTED");
		HTTP.searchTest(basePath + "?blacklist=never");
		HTTP.searchTest(basePath + "?blacklist=currently");
		HTTP.searchTest(basePath + "?blacklist=previously");
		HTTP.searchTest(basePath + "?after=0");
		HTTP.searchTest(basePath + "?before=0");
		HTTP.searchTest(basePath + "?after=0&before=0");
		
		HTTP.errorTest(basePath + "?blacklist=whenever", Error.BAD_FORMAT);
		HTTP.errorTest(basePath + "?after=NAN", Error.BAD_FORMAT);
		HTTP.errorTest(basePath + "?before=NAN", Error.BAD_FORMAT);
	}	
	
	@Test
	public void searchHostsTest() {
		String basePath = "/v2/search/hosts";
		
		HTTP.searchTest(basePath + "?host=example.com");
		HTTP.searchTest(basePath + "?host=.*%5B%5E.%5Dexample.com");
		HTTP.searchTest(basePath + "?resolvesto=0");
		HTTP.searchTest(basePath + "?hasresolvedto=0");
		
		HTTP.errorTest(basePath + "?resolvesto=NAN", Error.BAD_FORMAT);
		HTTP.errorTest(basePath + "?hasresolvedto=NAN", Error.BAD_FORMAT);
	}
	
	@Test
	public void searchIPsTest() {
		String basePath = "/v2/search/ips";
		HTTP.searchTest(basePath + "?ip=0");
		HTTP.searchTest(basePath + "?ip=0.0.0.0");
	}
	
	@Test
	public void searchASNsTest() {
		String basePath = "/v2/search/asns";
		HTTP.searchTest(basePath + "?name=TEST");
		HTTP.searchTest(basePath + "?number=0");
		HTTP.searchTest(basePath + "?country=TEST");
		HTTP.errorTest(basePath + "?number=NAN", Error.BAD_FORMAT);
	}
	
	@Test
	public void searchNotFoundTest() {
		String path = "/v2/search/thereisnospoon";
		TestResponse res = HTTP.getTest(path);
		assertTrue(res.code == NOT_FOUND);
	}

}
