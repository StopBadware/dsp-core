package org.stopbadware.dsp.data;

import static org.junit.Assert.*;


import org.junit.Test;

import com.mongodb.DB;

public class MongoDbTest {

	@Test
	public void testSwitchToTestDB() {
		String origName = MongoDb.getDB().getName();
		assertTrue(!origName.equalsIgnoreCase("testdb"));
		String newName = MongoDb.getDB().getName();
		assertTrue(newName.equalsIgnoreCase("testdb"));
	}

	@Test
	public void testGetDB() {
		DB db = MongoDb.getDB();
		assertTrue(db instanceof DB);
	}

}
