package org.stopbadware.dsp.data;

import static org.junit.Assert.*;
import org.junit.Test;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.dsp.test.helpers.AuthAuthTestHelper;
import org.apache.shiro.subject.Subject;

public class AsnsHandlerTest {
	
	private Subject subject = AuthAuthTestHelper.getSubject();
	private AsnsHandler asns = new AsnsHandler(MongoDb.getDB(), subject);
	
	@Test
	public void testGetAS() {
		assertTrue(subject.isPermitted(Permissions.READ_ASNS));
	}
	
	@Test
	public void testAddAutonmousSystem() {
		
	}

}
