package org.stopbadware.dsp;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

public class ShareLevelTest {

	@Test
	public void castFromStringTest() {
		assertTrue(ShareLevel.value("SBW") == ShareLevel.SBW_ONLY);
		assertTrue(ShareLevel.valueOf("SBW_ONLY") == ShareLevel.SBW_ONLY);
	}

	@Test
	public void getLeastRestrictiveTest() {
		assertTrue(ShareLevel.PUBLIC.compareTo(ShareLevel.SBW_ONLY) > 0);
		assertTrue(ShareLevel.getLeastRestrictive(ShareLevel.PUBLIC, ShareLevel.SBW_ONLY)==ShareLevel.PUBLIC);
	}
	
	@Test
	public void getAllAboveTest() {
		List<ShareLevel> aboveSBW = ShareLevel.SBW_ONLY.getAllAbove();
		assertTrue(aboveSBW.size() > 0);
		assertTrue(aboveSBW.contains(ShareLevel.PUBLIC));
	}

}
