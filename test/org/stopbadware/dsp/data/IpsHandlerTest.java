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
		boolean added = ips.addIp(TEST_IP);
		assertTrue(added || ips.getIp(TEST_IP).getCount() > 0);
	}
	
	@Test
	public void getIpFromLongTest() {
		SearchResults sr = ips.getIp(TEST_IP);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getIpFromStringTest() {
		SearchResults sr = ips.getIp(IP.longToDots(TEST_IP));
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void addIpTest() {
		long notInDb = 0L;
		SearchResults sr = ips.getIp(notInDb);
		while (sr.getCount() != 0) {
			notInDb++;
			sr = ips.getIp(notInDb);
		}
		boolean added = ips.addIp(notInDb);
		sr = ips.getIp(notInDb);
		assertTrue(added);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void updateAsnTest() {
		ips.updateAsn(TEST_IP, PRIVATE_AS_RANGE_START);
		int updated = ips.updateAsn(TEST_IP, PRIVATE_AS_RANGE_END);
		assertTrue(updated > 0);
	}
	
}
