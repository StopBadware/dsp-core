package org.stopbadware.dsp.data;

import static org.junit.Assert.*;


import org.junit.Test;

import com.mongodb.DB;

public class MongoDbTest {

	@Test
	public void testGetDB() {
		DB db = MongoDb.getDB();
		assertTrue(db != null);
	}

}
