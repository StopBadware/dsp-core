package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.Subject;
import org.junit.Test;
import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.ERWrapper;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.RESTfulToken;
import org.stopbadware.dsp.sec.Realm;
import org.stopbadware.lib.util.SHA2;

public class DBHandlerTest {
	
	private static DBHandler dbh = null; 
	private static Realm realm = new Realm();
	private static SecurityManager securityManager = new DefaultSecurityManager(realm);
	private static final String TEST_SOURCE = "TESTING";
	private static final String TEST_HOST = "example.com";
	private static final long TEST_IP = 47L;
	private static final int TEST_ASN = 18;
	
	static {
		realm.setCachingEnabled(false);
		SecurityUtils.setSecurityManager(securityManager);
		Subject subject = SecurityUtils.getSubject();
		String key = "DATA123456";
		String sig = "54fc7ffd3cdc856c09c8747b61718741166f347b93f43c8db2ce6e4f568881e1";
		String path = "/clearinghouse/events/test";
		long ts = 1294513200L;
		
		RESTfulToken token = new RESTfulToken(key, sig, path, ts); 
		try {
			subject.login(token);
		} catch (AuthenticationException e) {
			fail("AuthenticationException thrown");
		}
		try {
			MongoDB.switchToTestDB();
		} catch (UnknownHostException e) {
			fail("UnknownHostException thrown when switching to test db");
		}
		dbh = new DBHandler(subject);
	}

	@Test
	public void testGetTimeOfLast() {
		TimeOfLast tol = dbh.getTimeOfLast(TEST_SOURCE);
		assertTrue(tol != null && tol instanceof TimeOfLast);
		assertTrue(tol.getLast() >= 0 && tol.getLast() < Long.MAX_VALUE);
		assertTrue(tol.getSource().equalsIgnoreCase(TEST_SOURCE));
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
		erMap.put("reported_by", TEST_SOURCE);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("host", TEST_HOST);
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
		erMap.put("reported_by", TEST_SOURCE);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper erw = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(erw);
		int added = dbh.addEventReports(reports);
		if (added > 0) {
			Set<String> cleanHosts = new HashSet<>();
			cleanHosts.add(TEST_HOST);
			int cleaned = dbh.updateBlacklistFlagsFromCleanHosts(TEST_SOURCE, System.currentTimeMillis()/1000, cleanHosts);
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
		erMap.put("sha2_256", SHA2.get256("Making your way in the world today takes everything you got..."));
		erMap.put("reported_by", TEST_SOURCE);
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
		erMap.put("reported_by", TEST_SOURCE);
		erMap.put("reported_at", System.currentTimeMillis()/1000);
		erMap.put("is_on_blacklist", true);
		erMap.put("host", TEST_HOST);
		ERWrapper newERW = new ERWrapper(TEST_HOST, ShareLevel.PUBLIC.toString(), erMap);
		reports.add(newERW);
		
		int cleaned = dbh.updateBlacklistFlagsFromDirtyReports(TEST_SOURCE, System.currentTimeMillis()/1000, reports);
		assertTrue(cleaned > 0);
	}

}
