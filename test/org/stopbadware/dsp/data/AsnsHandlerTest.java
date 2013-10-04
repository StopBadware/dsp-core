package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import org.junit.Test;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.apache.shiro.subject.Subject;

public class AsnsHandlerTest {
	
	private static Subject subject = AuthAuthTestHelper.getSubject();
	private static AsnsHandler asns = new AsnsHandler(MongoDb.getDB(), subject);
	
	@Test
	public void testGetAS() {
		SearchResults sr = asns.getAS(15169);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
		assertTrue(sr.getResults() != null);
	}
	
	@Test
	public void testAddAutonmousSystem() {
		AutonomousSystem as = new AutonomousSystem();
		int testNum = 65535;
		SearchResults sr = asns.getAS(testNum);
		while (sr.getCount() != 0) {
			sr = asns.getAS(testNum++);
		}
		as.setAsn(testNum);
		as.setCountry("TEST");
		as.setName("TEST");
		boolean added = asns.addAutonmousSystem(as);
		assertTrue(added);
		SearchResults confirm = asns.getAS(as.getAsn());
		assertTrue(confirm.getCount() > 0);
	}

}
