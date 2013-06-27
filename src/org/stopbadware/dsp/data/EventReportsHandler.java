package org.stopbadware.dsp.data;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public class EventReportsHandler extends MDBCollectionHandler {
	
	public EventReportsHandler(DB db, DBCollection coll, boolean canRead, boolean canWrite) {
		super(db, coll, canRead, canWrite);
		delme();	//DELME
	}
	
	private void delme() {	//DELME
		System.out.println("MDB:"+db.toString());
		System.out.println("coll:"+coll.toString());
		System.out.println("can read:"+canRead);
		System.out.println("can write:"+canWrite);
	}
	
}
