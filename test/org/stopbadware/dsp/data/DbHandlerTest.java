package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Test;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.stopbadware.lib.util.SHA2;

public class DbHandlerTest {
	
	private static DbHandler dbh = new DbHandler(AuthAuthTestHelper.getSubject()); 
	private static EventReportsHandler er = new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	private static final String TEST_PREFIX = "test";
	private static final String TEST_HOST = "example.com";
	
	@Test
	public void getTimeOfLastTest() {
		TimeOfLast tol = dbh.getTimeOfLast(TEST_PREFIX);
		assertTrue(tol != null);
		assertTrue(tol.getLast() >= 0 && tol.getLast() < Long.MAX_VALUE);
		assertTrue(tol.getSource().equalsIgnoreCase(TEST_PREFIX));
	}

	@Test
	public void getCurrentlyBlacklistedHostsTest() {
		Set<String> hosts = dbh.getCurrentlyBlacklistedHosts();
		assertTrue(hosts != null);
		assertTrue(hosts.size() > 0);
	}

	@Test
	public void addEventReportsTest() {
		Set<ERWrapper> reports = new HashSet<>();
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("A long time ago, in a galaxy far, far away..."));
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("host", TEST_HOST);
		ERWrapper erw = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		assertTrue(added > 0);
	}
	
	@Test
	public void addEventReportsWithUpdateTest() {
		Map<String, Object> origMap = new HashMap<>();
		origMap.put("sha2_256", SHA2.get256("It doesn't feel pity, or remorse, or fear."));
		origMap.put("prefix", TEST_PREFIX);
		origMap.put("reported_at", System.currentTimeMillis()/1000);
		origMap.put("host", TEST_HOST);
		Map<String, Object> blacklistedMap = new HashMap<>();
		blacklistedMap.putAll(origMap);
		blacklistedMap.put("is_on_blacklist", true);
		blacklistedMap.put("removed_from_blacklist", 0);
		blacklistedMap.put(TEST_PREFIX+"_model", 101);
		Map<String, Object> cleanMap = new HashMap<>();
		cleanMap.putAll(origMap);
		cleanMap.put("is_on_blacklist", false);
		cleanMap.put("removed_from_blacklist", System.currentTimeMillis()/1000);
		addEventReport(blacklistedMap.get("host").toString(), blacklistedMap);
		long removed = getRemovedFromBlacklistTime(origMap);
		assertTrue(removed == Long.valueOf(blacklistedMap.get("removed_from_blacklist").toString()));
		addEventReport(cleanMap.get("host").toString(), cleanMap);
		removed = getRemovedFromBlacklistTime(origMap);
		assertTrue(removed == Long.valueOf(cleanMap.get("removed_from_blacklist").toString()));
	}
	
	private void addEventReport(String host, Map<String, Object> erMap) {
		Set<ERWrapper> reports = new HashSet<>();
		ERWrapper erw = new ERWrapper(host, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		assertTrue(added > 0);
	}
	
	private long getRemovedFromBlacklistTime(Map<String, Object> erMap) {
		long removed = -1;
		SearchResults sr = er.getEventReport(erMap.get("prefix").toString(), erMap.get("sha2_256").toString(), Long.valueOf(erMap.get("reported_at").toString()));
		assertTrue(sr != null);
		Object results = sr.getResults();
		assertTrue(results != null);
		assertTrue(results instanceof List<?>);
		if (results instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<Map<String, Object>> resultList = (List<Map<String, Object>>) results;
			assertTrue(resultList.size() > 0);
			assertTrue(resultList.get(0).containsKey("removed_from_blacklist"));
			removed = Long.valueOf(resultList.get(0).get("removed_from_blacklist").toString());
		}
		return removed;
	}

	@Test
	public void addIPsForHostsTest() {
		Map<String, Long> ips = new HashMap<>();
		/* set to zero to ensure subsequent call should result in a db write */
		ips.put(TEST_HOST, 0L);
		dbh.addIPsForHosts(ips);
		ips.clear();
		ips.put(TEST_HOST, 1L);
		int added = dbh.addIPsForHosts(ips);
		assertTrue(added > 0);
	}

	@Test
	public void addASNsForIPsTest() {
		Map<Long, AutonomousSystem> asns = new HashMap<>();
		AutonomousSystem asn = new AutonomousSystem();
		int testAsn = PRIVATE_AS_RANGE_START;
		int added = 0;
		while (added == 0 && testAsn < PRIVATE_AS_RANGE_END) {
			asn.setAsn(testAsn++);
			asns.put(0L, asn);
			added = dbh.addASNsForIPs(asns);
			asns = new HashMap<>();
		}
		assertTrue(added > 0);
	}

	@Test
	public void updateBlacklistFlagsFromCleanHostsTest() {
		Set<ERWrapper> reports = new HashSet<>();
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("Space, the final frontier, these are the voyages..."));
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper erw = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		if (added > 0) {
			Set<String> cleanHosts = new HashSet<>();
			cleanHosts.add(TEST_HOST);
			int cleaned = dbh.updateBlacklistFlagsFromCleanHosts(TEST_PREFIX, System.currentTimeMillis()/1000, cleanHosts);
			assertTrue(cleaned > 0);
		} else {
			fail("Unable to add test event report");
		}
	}

	@Test
	public void updateBlacklistFlagsFromDirtyReportsTest() {
		Set<ERWrapper> reports = new HashSet<>();
		/* to be cleaned */
		Map<String, Object> erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("Making your way in the world today takes everything you got..."));
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", "cleanme.test");
		ERWrapper oldERW = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(oldERW);
		int added = dbh.addEventReports(reports);
		if (added <= 0) {
			fail("Unable to add test event report to mark clean");
		}
		
		reports = new HashSet<>();
		/* dirty placeholder */
		erMap = new HashMap<>();
		erMap.put("sha2_256", SHA2.get256("So no one told you life was gonna be this way..."));
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper newERW = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(newERW);
		
		int cleaned = dbh.updateBlacklistFlagsFromDirtyReports(TEST_PREFIX, System.currentTimeMillis()/1000, reports);
		assertTrue(cleaned > 0);
	}

}
