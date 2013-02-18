package org.stopbadware.dsp.data;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mongodb.DB;

/**
 * Class to handle all security related database events 
 */
public class SecurityDBHandler {
	
	private DB secdb;
	private static final Logger LOG = LoggerFactory.getLogger(SecurityDBHandler.class);
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;
	
	public SecurityDBHandler() {
		secdb = MongoDB.getSecurityDB();
	}
	
	public String getSecret(String apiKey) {
		//TODO: DATA-54 get secret key from db
		return "SECRET";
	}
}
