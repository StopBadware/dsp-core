package org.stopbadware.dsp;

import static org.junit.Assert.*;

import org.junit.Test;

public class ShareLevelTest {

	@Test
	public void testCastFromString() {
		assertTrue(ShareLevel.castFromString("SBW") == ShareLevel.SBW_ONLY);
	}

	@Test
	public void testGetLeastRestrictive() {
		assertTrue(ShareLevel.PUBLIC.compareTo(ShareLevel.SBW_ONLY) > 0);
		assertTrue(ShareLevel.getLeastRestrictive(ShareLevel.PUBLIC, ShareLevel.SBW_ONLY)==ShareLevel.PUBLIC);
	}

}
