package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.RateLimitException;
import org.stopbadware.dsp.SearchException;
import org.stopbadware.dsp.data.DbHandler.WriteStatus;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.stopbadware.lib.util.SHA2;

import static org.stopbadware.dsp.test.helpers.TestVals.*;

public class EventReportsHandlerTest {
	
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
		WriteStatus ws = erHandler().addEventReport(erTestMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
	}
	
	@Test
	public void findEventReportsSinceTimeTest() throws RateLimitException {
		SearchResults sr = erHandler().findEventReportsSince(twentyFourHoursAgo);
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
	public void findEventReportsSinceReportTest() throws RateLimitException {
		String uid = Long.toHexString(twentyFourHoursAgo) + TEST;
		try {
			SearchResults sr = erHandler().findEventReportsSince(uid);
			assertTrue(sr.getCode() == SearchResults.OK);
			assertTrue(sr.getCount() > 0);
		} catch (SearchException e) {
			fail("SearchException thrown" + e.getMessage());
		}
		
	}
	
	@Test(expected = SearchException.class)
	public void findEventReportsSinceReportInvalidStringTest() throws SearchException, RateLimitException {
		erHandler().findEventReportsSince("");
	}
	
	@Test(expected = SearchException.class)
	public void findEventReportsSinceReportInvalidTimeTest() throws SearchException, RateLimitException {
		erHandler().findEventReportsSince("XXXXXXXX3bf4421940d5029c");
	}
	
	@Test(expected = RateLimitException.class)
	public void findEventReportsRateLimitTest() throws RateLimitException {
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
	public void findEventReportsRateLimitResetTest() {
		boolean accountIsRateLimited = false;
		try {
			for (int i=0; i<Integer.valueOf(System.getenv("RATE_LIMIT_MAX"))*2; i++) {
				new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getRateLimitSubject()).findEventReportsSince(twentyFourHoursAgo);
				Thread.sleep(50);
			}
		} catch (InterruptedException e) {
			fail("InterruptedException thrown: " + e.getMessage());
		} catch (RateLimitException e) {
			accountIsRateLimited = true;
		}
		assertTrue(accountIsRateLimited);
		try {
			long sleepDur = (Long.valueOf(System.getenv("RATE_LIMIT_SECONDS")) + 1) * 1000;
			Thread.sleep(sleepDur);
		} catch (NumberFormatException e) {
			fail("Invalid RATE_LIMIT_SECONDS: " + e.getMessage());
		} catch (InterruptedException e) {
			fail("InterruptedException thrown: " + e.getMessage());
		}
		SearchResults sr = null;
		try {
			sr = new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getRateLimitSubject()).findEventReportsSince(twentyFourHoursAgo);
		} catch (RateLimitException e) {
			fail("RateLimitException thrown: " + e.getMessage());
		}
		assertTrue(sr != null);
	}
	
	@Test
	public void getEventReportsStatsTest() {
		SearchResults sr = erHandler().getEventReportsStats(TEST_PREFIX);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
	}
	
	@Test
	public void getEventReportTest() throws RateLimitException {
		EventReportsHandler er = erHandler();
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
		TimeOfLast tol = erHandler().getTimeOfLast(TEST_PREFIX);
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
		WriteStatus ws = erHandler().addEventReport(erMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
	}
	
	@Test
	public void getCurrentlyBlacklistedHostsTest() {
		Set<String> blacklisted = erHandler().getCurrentlyBlacklistedHosts();
		assertTrue(blacklisted.size() > 0);
		assertTrue(blacklisted.contains(TEST_HOST));
	}
	
	@Test
	public void getHostsWithEventReportsSinceTest() {
		Set<String> hosts = erHandler().getHostsWithEventReportsSince(0);
		assertTrue(hosts.size() > 0);
		assertTrue(hosts.contains(TEST_HOST));
	}
	
	@Test
	public void findCurrentlyBlacklistedBySourceTest() {
		Set<String> blacklisted = erHandler().findCurrentlyBlacklistedBySource(TEST_PREFIX, "host");
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
		EventReportsHandler er = erHandler();
		WriteStatus ws = er.addEventReport(erMap);
		assertTrue(ws.equals(WriteStatus.SUCCESS));
		int removed = er.removeFromBlacklist(TEST_PREFIX, "sha2_256", hash, System.currentTimeMillis()/1000);
		assertTrue(removed > 0);
	}
	
	@Test
	public void getParticipantPrefixesTest() {
		SearchResults sr = erHandler().getParticipantPrefixes();
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
		if (rawResults instanceof List<?>) {
			results = (List<Map<String, Object>>) rawResults;
		} else {
			results = new ArrayList<>();
		}
		return results;
	}
	
	private static EventReportsHandler erHandler() {
		return new EventReportsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	}

}
