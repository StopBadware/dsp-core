package org.stopbadware.dsp.data;

import static org.junit.Assert.*;

import java.net.UnknownHostException;

import org.junit.Test;

import com.mongodb.DB;

public class MongoDBTest {

	@Test
	public void testSwitchToTestDB() {
		String origName = MongoDB.getDB().getName();
		assertTrue(!origName.equalsIgnoreCase("testdb"));
		try {
			MongoDB.switchToTestDB();
		} catch (UnknownHostException e) {
			fail("UnknownHostException thrown when switching to test db");
		}
		String newName = MongoDB.getDB().getName();
		assertTrue(newName.equalsIgnoreCase("testdb"));
	}

	@Test
	public void testGetDB() {
		DB db = MongoDB.getDB();
		assertTrue(db instanceof DB);
	}

}
