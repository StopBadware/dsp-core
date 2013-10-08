package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import static org.stopbadware.dsp.test.helpers.TestVals.*;

import org.junit.BeforeClass;
import org.junit.Test;
import org.stopbadware.dsp.json.AutonomousSystem;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;

public class AsnsHandlerTest {
	
	private static AsnsHandler asns = new AsnsHandler(MongoDb.getDB(), AuthAuthTestHelper.getSubject());
	
	@BeforeClass
	public static void addTestAS() {
		AutonomousSystem as = new AutonomousSystem();
		as.setAsn(PRIVATE_AS_RANGE_START);
		boolean added = asns.addAutonmousSystem(as);
		assertTrue(added);
	}
	
	@Test
	public void getASTest() {
		SearchResults sr = asns.getAS(PRIVATE_AS_RANGE_START);
		assertTrue(sr.getCode() == SearchResults.OK);
		assertTrue(sr.getCount() > 0);
		assertTrue(sr.getResults() != null);
	}
	
	@Test
	public void addAutonmousSystemTest() {
		AutonomousSystem as = new AutonomousSystem();
		int testNum = PRIVATE_AS_RANGE_START;
		SearchResults sr = asns.getAS(testNum);
		while (sr.getCount() != 0) {
			sr = asns.getAS(testNum++);
		}
		as.setAsn(testNum);
		as.setCountry(TEST);
		as.setName(TEST);
		boolean added = asns.addAutonmousSystem(as);
		assertTrue(added);
		SearchResults confirm = asns.getAS(as.getAsn());
		assertTrue(confirm.getCount() > 0);
	}

}
