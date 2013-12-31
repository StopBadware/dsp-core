package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler.WriteStatus;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.stopbadware.lib.util.SHA2;

public class EventReportsHandlerTest {
	
	private static EventReportsHandler er = new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	private static final String TEST_PREFIX = TEST;
	private static final String TEST_HOST = "example.com";
	private static long twentyFourHoursAgo = (System.currentTimeMillis()/1000) - (60*60*24);
	private static Map<String, Object> erTestMap = new HashMap<>();
	
	@BeforeClass
	public static void addTestReport() {
		erTestMap.put("sha2_256", SHA2.get256("For the Horde!"));
		erTestMap.put("is_on_blacklist", true);
		erTestMap.put("prefix", TEST_PREFIX);
		erTestMap.put("reported_at", System.currentTimeMillis()/1000);
		erTestMap.put("host", TEST_HOST);
		WriteStatus ws = er.addEventReport(erTestMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
	}
	
	@Test
	public void findEventReportsSinceTimeTest() {
		SearchResults sr = er.findEventReportsSince(twentyFourHoursAgo);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void findEventReportsSinceReportTest() {
		String uid = ""; //TODO DATA-138
		SearchResults sr = er.findEventReportsSince(uid);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getEventReportsStatsTest() {
		SearchResults sr = er.getEventReportsStats(TEST_PREFIX);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getEventReportTest() {
		SearchResults sr;
		String uid = erTestMap.get("sha2_256")+"-"+TEST_PREFIX+"-"+erTestMap.get("reported_at");
		try {
			sr = er.getEventReport(uid);
			assertTrue(sr.getCode() == SearchResults.OK);
			assertTrue(sr.getCount() > 0);
		} catch (SearchException e) {
			fail("SearchException thrown");
		}
		
	}
	
	@Test
	public void getTimeOfLastTest() {
		TimeOfLast tol = er.getTimeOfLast(TEST_PREFIX);
		assertTrue(tol.getLast() == (long) erTestMap.get("reported_at"));
	}
	
	@Test
	public void addEventReportTest() {
		Map<String, Object> erMap = new HashMap<>();
		String hash = SHA2.get256("Leeeeeeeeeeeeeroy Jenkins!");
		erMap.put("sha2_256", hash);
		erMap.put("is_on_blacklist", true);
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("host", TEST_HOST);
		WriteStatus ws = er.addEventReport(erMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
	}
	
	@Test
	public void getCurrentlyBlacklistedHostsTest() {
		Set<String> blacklisted = er.getCurrentlyBlacklistedHosts();
		assertTrue(blacklisted.size() > 0);
		assertTrue(blacklisted.contains(TEST_HOST));
	}
	
	@Test
	public void getHostsWithEventReportsSinceTest() {
		Set<String> hosts = er.getHostsWithEventReportsSince(0);
		assertTrue(hosts.size() > 0);
		assertTrue(hosts.contains(TEST_HOST));
	}
	
	@Test
	public void findCurrentlyBlacklistedBySourceTest() {
		Set<String> blacklisted = er.findCurrentlyBlacklistedBySource(TEST_PREFIX, "host");
		assertTrue(blacklisted.size() > 0);
		assertTrue(blacklisted.contains(TEST_HOST));
	}
	
	@Test
	public void removeFromBlacklistTest() {
		Map<String, Object> erMap = new HashMap<>();
		String hash = SHA2.get256("For the Forsaken!");
		erMap.put("sha2_256", hash);
		erMap.put("is_on_blacklist", true);
		erMap.put("prefix", TEST_PREFIX);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("host", TEST_HOST);
		WriteStatus ws = er.addEventReport(erMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
		int removed = er.removeFromBlacklist(TEST_PREFIX, "sha2_256", hash, System.currentTimeMillis()/1000);
		assertTrue(removed > 0);
	}
	
	@Test
	public void getParticipantPrefixesTest() {
		SearchResults sr = er.getParticipantPrefixes();
		assertTrue(sr != null);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
		Object results = sr.getResults();
		if (results instanceof List<?>) {
			@SuppressWarnings("unchecked")
			List<Map<String, String>> resultList = (List<Map<String, String>>) results;
			if (!resultList.isEmpty() && resultList.get(0) instanceof Map<?, ?>) {
				Map<String, String> prefixes = (Map<String, String>) resultList.get(0);
				assertTrue(!prefixes.isEmpty());
				assertTrue(prefixes.containsKey(TEST_PREFIX));
			} else {
				fail("Results do not include expected prefix=>particpant map");
			}
		} else {
			fail("Unexpected results collection");
		}
	}

}
