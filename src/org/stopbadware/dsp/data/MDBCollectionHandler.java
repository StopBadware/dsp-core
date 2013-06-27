package org.stopbadware.dsp.data;

import com.mongodb.DB;
import com.mongodb.DBCollection;

public abstract class MDBCollectionHandler {
	
	protected DB db;
	protected DBCollection coll;
	protected boolean canRead = false;
	protected boolean canWrite = false;
	
	protected static final String DUPE_ERR = "E11000";	//DELME?
	protected static final int ASC = MongoDB.ASC;		//DELME?
	protected static final int DESC = MongoDB.DESC;		//DELME?
	
	protected MDBCollectionHandler(DB db, DBCollection coll, boolean canRead, boolean canWrite) {
		this.db = db;
		this.coll = coll;
		this.canRead = canRead;
		this.canWrite = canWrite;
	}

}
