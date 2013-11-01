package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.stopbadware.lib.util.IP;

public class IpsHandlerTest {
	
	private static IpsHandler ips = new IpsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	private static final long TEST_IP = 0L;
	
	@BeforeClass
	public static void addTestIp() {
		boolean added = ips.addIP(TEST_IP);
		assertTrue(added || ips.getIP(TEST_IP).getCount() > 0);
	}
	
	@Test
	public void getIpFromLongTest() {
		SearchResults sr = ips.getIP(TEST_IP);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getIpFromStringTest() {
		SearchResults sr = ips.getIP(IP.longToDots(TEST_IP));
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void addIpTest() {
		long notInDb = 0L;
		SearchResults sr = ips.getIP(notInDb);
		while (sr.getCount() != 0) {
			sr = ips.getIP(notInDb++);
		}
		boolean added = ips.addIP(notInDb);
		sr = ips.getIP(notInDb);
		assertTrue(added);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void updateAsnTest() {
		ips.updateASN(TEST_IP, PRIVATE_AS_RANGE_START);
		int updated = ips.updateASN(TEST_IP, PRIVATE_AS_RANGE_END);
		assertTrue(updated > 0);
	}
	
}
