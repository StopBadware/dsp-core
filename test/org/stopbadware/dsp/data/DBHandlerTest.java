package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.lib.util.SHA2;

public class DBHandlerTest {
	
	private DBHandler dbh = new DBHandler(); 
	private static final String TEST_HOST = "example.com";
	private static final long TEST_IP = 47L;
	private static final int TEST_ASN = 18;

	@Before
	public void setUp() throws Exception {
		MongoDB.switchToTestDB();
	}

	@Test
	public void testGetTimeOfLast() {
		TimeOfLast tol = dbh.getTimeOfLast("test");
		assertTrue(tol != null && tol instanceof TimeOfLast);
		assertTrue(tol.getLast() >= 0 && tol.getLast() < Long.MAX_VALUE);
		assertTrue(tol.getSource().equalsIgnoreCase("test"));
	}

	@Test
	public void testGetCurrentlyBlacklistedHosts() {
		Set<String> hosts = dbh.getCurrentlyBlacklistedHosts();
		assertTrue(hosts != null && hosts instanceof HashSet);
	}

	@Test
	public void testAddEventReports() {
		Set<ERWrapper> reports = new HashSet<>();
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("A long time ago, in a galaxy far, far away..."));
		erMap.put("reported_by", "TEST");
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		ERWrapper erw = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		assertTrue(added > 0);
	}

	@Test
	public void testAddIPsForHosts() {
		Map<String, Long> ips = new HashMap<>();
		ips.put(TEST_HOST, 0L);
		dbh.addIPsForHosts(ips);
		/* set to zero to ensure next call should result in a db write */
		ips.put(TEST_HOST, TEST_IP);
		int added = dbh.addIPsForHosts(ips);
		assertTrue(added > 0);
	}

	@Test
	public void testAddASNsForIPs() {
		Map<Long, AutonomousSystem> asns = new HashMap<>();
		AutonomousSystem asn = new AutonomousSystem();
		asn.setAsn(0);
		asns.put(TEST_IP, asn);
		/* set to zero to ensure next call should result in a db write */
		dbh.addASNsForIPs(asns);
		asn.setAsn(TEST_ASN);
		asns.put(TEST_IP, asn);
		int added = dbh.addASNsForIPs(asns);
		assertTrue(added > 0);
	}

	@Test
	public void testUpdateBlacklistFlagsFromCleanHosts() {
		Set<ERWrapper> reports = new HashSet<>();
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("Space, the final frontier, these are the voyages..."));
		erMap.put("reported_by", "TEST");
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper erw = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		if (added > 0) {
			Set<String> cleanHosts = new HashSet<>();
			cleanHosts.add(TEST_HOST);
			int cleaned = dbh.updateBlacklistFlagsFromCleanHosts("TEST", System.currentTimeMillis()/1000, cleanHosts);
			assertTrue(cleaned > 0);
		} else {
			fail("Unable to add test event report");
		}
	}

	@Test
	public void testUpdateBlacklistFlagsFromDirtyReports() {
		Set<ERWrapper> reports = new HashSet<>();
		/* to be cleaned */
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("...sometimes you want to go, where everybody knows your name..."));
		erMap.put("reported_by", "TEST");
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", "cleanme.com");
		ERWrapper oldERW = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(oldERW);
		/* dirty placeholder */
		erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("Space, the final frontier, these are the voyages..."));
		erMap.put("reported_by", "TEST");
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper newERW = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(newERW);
		int added = dbh.addEventReports(reports);
	}

}
