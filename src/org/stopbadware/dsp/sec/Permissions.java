package org.stopbadware.dsp.sec;

import org.stopbadware.dsp.ShareLevel;
import org.stopbadware.dsp.data.MongoDB;

/**
 * 
 *
 */
public abstract class Permissions {

	private static final String READ = "read";
	private static final String WRITE = "write";
	
	public static final String READ_ASNS = MongoDB.ASNS+":"+READ;
	public static final String READ_EVENTS = MongoDB.EVENT_REPORTS+":"+READ;
	public static final String READ_HOSTS = MongoDB.HOSTS+":"+READ;
	public static final String READ_IPS = MongoDB.IPS+":"+READ;
	
	public static final String WRITE_ACCOUNTS = MongoDB.ACCOUNTS+":"+WRITE;
	public static final String WRITE_ASNS = MongoDB.ASNS+":"+WRITE;
	public static final String WRITE_EVENTS = MongoDB.EVENT_REPORTS+":"+WRITE;
	public static final String WRITE_HOSTS = MongoDB.HOSTS+":"+WRITE;
	public static final String WRITE_IPS = MongoDB.IPS+":"+WRITE;
	
	public static final String SL_SBW_ONLY = "share_level:"+ShareLevel.SBW_ONLY.toString().toLowerCase();
	public static final String SL_DSP_ONLY = "share_level:"+ShareLevel.DSP_ONLY.toString().toLowerCase();
	public static final String SL_RESEARCH = "share_level:"+ShareLevel.RESEARCH.toString().toLowerCase();
	public static final String SL_TAKEDOWN = "share_level:"+ShareLevel.TAKEDOWN.toString().toLowerCase();
	public static final String SL_PUBLIC = "share_level:"+ShareLevel.PUBLIC.toString().toLowerCase();
	
}
