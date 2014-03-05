package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
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

import static org.stopbadware.dsp.test.helpers.TestVals.*;

public class EventReportsHandlerTest {
	
	private static EventReportsHandler er = new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	private static final String TEST_PREFIX = "test";
	private static final String TEST_HOST = "example.com";
	private static long twentyFourHoursAgo = (System.currentTimeMillis()/1000) - (60*60*24);
	private static Map<String, Object> erTestMap = new HashMap<>();
	
	@BeforeClass
	public static void addTestReport() {
		erTestMap.put("sha2_256", SHA2.get256("For the Horde!"+System.nanoTime()));
		erTestMap.put("is_on_blacklist", true);
		erTestMap.put("prefix", TEST_PREFIX);
		erTestMap.put("reported_by", TEST_PREFIX.toUpperCase());
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
		List<Map<String, Object>> results = getResults(sr.getResults());
		assertTrue(!results.isEmpty());
		for (Map<String, Object> result : results) {
			assertTrue(result.containsKey("uid"));
			assertTrue(!result.containsKey("_id"));
		}
	}
	
	@Test
	public void findEventReportsSinceReportTest() {
		String uid = Long.toHexString(twentyFourHoursAgo) + TEST;
		try {
			SearchResults sr = er.findEventReportsSince(uid);
			assertTrue(sr.getCode() == SearchResults.OK);
			assertTrue(sr.getCount() > 0);
		} catch (SearchException e) {
			fail("SearchException thrown" + e.getMessage());
		}
		
	}
	
	@Test(expected = SearchException.class)
	public void findEventReportsSinceReportInvalidStringTest() throws SearchException {
		er.findEventReportsSince("");
	}
	
	@Test(expected = SearchException.class)
	public void findEventReportsSinceReportInvalidTimeTest() throws SearchException {
		er.findEventReportsSince("XXXXXXXX3bf4421940d5029c");
	}
	
	@Test(expected = SearchException.class)
	public void findEventReportsRateLimitTest() {
		for (int i=0; i<Integer.valueOf(System.getenv("RATE_LIMIT_MAX"))*2; i++) {
			new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getRateLimitSubject()).findEventReportsSince(twentyFourHoursAgo);
			try {
				Thread.sleep(50);
			} catch (InterruptedException e) {
				fail("InterruptedException thrown: " + e.getMessage());
			}
		}
	}
	
	@Test
	public void getEventReportsStatsTest() {
		SearchResults sr = er.getEventReportsStats(TEST_PREFIX);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getEventReportTest() {
		Map<String, Object> head = getResults(er.findEventReportsSince(twentyFourHoursAgo).getResults()).get(0);
		SearchResults sr;
		try {
			sr = er.getEventReport(head.get("uid").toString());
			assertTrue(sr.getCode() == SearchResults.OK);
			assertTrue(sr.getCount() == 1);
			Map<String, Object> result = getResults(sr.getResults()).get(0);
			assertTrue(result.keySet().size() == head.keySet().size());
			for (String key : head.keySet()) {
				assertTrue(result.containsKey(key));
				assertTrue(result.get(key).equals(head.get(key)));
			}
		} catch (SearchException e) {
			fail("SearchException thrown: "+e.getMessage());
		}
	}
	
	@Test
	public void getTimeOfLastTest() {
		addTestReport();
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
		List<Map<String, Object>> results = getResults(sr.getResults());
		if (!results.isEmpty() && results.get(0) instanceof Map<?, ?>) {
			Map<String, Object> prefixes = (Map<String, Object>) results.get(0);
			assertTrue(!prefixes.isEmpty());
			assertTrue(prefixes.containsKey(TEST_PREFIX));
		} else {
			fail("Results do not include expected prefix=>particpant map");
		}
	}
	
	@SuppressWarnings("unchecked")
	private List<Map<String, Object>> getResults(Object rawResults) {
		List<Map<String, Object>> results = null; 
		System.out.println(rawResults instanceof List<?>);	//DELME DATA-122
		System.out.println(rawResults == null);				//DELME DATA-122
		if (rawResults instanceof List<?>) {
			results = (List<Map<String, Object>>) rawResults;
		} else {
			results = new ArrayList<>();
		}
		return results;
	}

}
