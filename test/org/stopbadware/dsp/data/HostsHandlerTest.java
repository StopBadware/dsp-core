package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;

public class HostsHandlerTest {
	
	private static HostsHandler hosts = new HostsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	private static final String TEST_HOST = "example.com";

	@BeforeClass
	public static void addTestHost() {
		hosts.addHost(TEST_HOST, ShareLevel.SBW_ONLY);
	}
	
	@Test
	public void getHostTest() {
		SearchResults sr = hosts.getHost(TEST_HOST);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void addHostTest() {
		String host = TEST+System.currentTimeMillis()+".com";
		boolean added = hosts.addHost(host, ShareLevel.SBW_ONLY);
		SearchResults sr = hosts.getHost(host);
		assertTrue(added);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void addIPForHostTest() {
		hosts.addIPForHost(TEST_HOST, 0L);
		boolean added = hosts.addIPForHost(TEST_HOST, 1L);
		assertTrue(added);
	}
}
