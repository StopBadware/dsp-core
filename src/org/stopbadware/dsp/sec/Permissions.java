package org.stopbadware.dsp.sec;

import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.data.MongoDb;

public abstract class Permissions {

	private static final String READ = "read";
	private static final String WRITE = "write";
	private static final String SHARE_LEVEL = "share_level";
	
	public static final String READ_ASNS = MongoDb.ASNS+":"+READ;
	public static final String READ_EVENTS = MongoDb.EVENT_REPORTS+":"+READ;
	public static final String READ_HOSTS = MongoDb.HOSTS+":"+READ;
	public static final String READ_IPS = MongoDb.IPS+":"+READ;
	
	public static final String WRITE_ACCOUNTS = MongoDb.ACCOUNTS+":"+WRITE;
	public static final String WRITE_ASNS = MongoDb.ASNS+":"+WRITE;
	public static final String WRITE_EVENTS = MongoDb.EVENT_REPORTS+":"+WRITE;
	public static final String WRITE_HOSTS = MongoDb.HOSTS+":"+WRITE;
	public static final String WRITE_IPS = MongoDb.IPS+":"+WRITE;
	
	public static final String SL_SBW_ONLY = SHARE_LEVEL+":"+ShareLevel.SBW_ONLY.toString().toLowerCase();
	public static final String SL_DSP_ONLY = SHARE_LEVEL+":"+ShareLevel.DSP_ONLY.toString().toLowerCase();
	public static final String SL_RESEARCH = SHARE_LEVEL+":"+ShareLevel.RESEARCH.toString().toLowerCase();
	public static final String SL_TAKEDOWN = SHARE_LEVEL+":"+ShareLevel.TAKEDOWN.toString().toLowerCase();
	public static final String SL_PUBLIC = SHARE_LEVEL+":"+ShareLevel.PUBLIC.toString().toLowerCase();
	
}
