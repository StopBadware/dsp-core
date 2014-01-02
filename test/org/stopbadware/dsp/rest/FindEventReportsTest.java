package org.stopbadware.dsp.rest;

import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.Test;
import org.stopbadware.dsp.json.Error;
import org.stopbadware.dsp.test.helpers.HttpRequestTestHelper;

public class FindEventReportsTest {
	
	private static final HttpRequestTestHelper HTTP = new HttpRequestTestHelper();

	@Test
	public void findSinceTest() {
		String basePath = "/v2/events/since/";
		HTTP.searchTest(basePath + System.currentTimeMillis()/1000);
		HTTP.errorTest(basePath + "NAN", Error.BAD_FORMAT);
	}
	
	@Test
	public void getLastReportedTimeTest() {
		String basePath = "/v2/events/timeoflast/";
		HTTP.searchTest(basePath + TEST);
	}
	
	@Test
	public void getParticipantPrefixesTest() {
		HTTP.searchTest("/v2/events/prefixes");
	}
	
	@Test
	public void findTest() {
		String basePath = "/v2/events/report/";
		HTTP.searchTest(basePath + VALID_ER_UID);
		HTTP.errorTest(basePath + TEST, Error.BAD_FORMAT);
	}
	
	@Test
	public void getStatsTest() {
		String basePath = "/v2/events/stats/";
		HTTP.searchTest(basePath + "all");
	}

}
