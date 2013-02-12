package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class DBHandlerTest {

	@Before
	public void setUp() throws Exception {
		MongoDB.switchToTestDB();
	}

	@Test
	public void testGetTimeOfLast() {
		fail("Not yet implemented");
	}

	@Test
	public void testGetCurrentlyBlacklistedHosts() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddEventReports() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddIPsForHosts() {
		fail("Not yet implemented");
	}

	@Test
	public void testAddASNsForIPs() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateBlacklistFlagsFromCleanHosts() {
		fail("Not yet implemented");
	}

	@Test
	public void testUpdateBlacklistFlagsFromDirtyReports() {
		fail("Not yet implemented");
	}

}
