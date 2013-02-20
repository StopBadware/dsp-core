package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
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
		//TODO: DATA-54 unencrypt and remove padding
		return "SECRET";
	}
	
	public String addUser(String apiKey, Set<String> roles) {
		//TODO: DATA-54 add user with roles as array, create secret
		//TODO: DATA-54 store secret encrypted with padding
		//TODO: DATA-54 return secret
		return "SECRET";
	}
	
	public Set<String> getRoles(String apiKey) {
		//TODO: DATA-54 get roles from db
		Set<String> roles = new HashSet<>();
		return roles;
	}
	
	//TODO: DATA-54 write permissions for roles in db
	public Set<Permission> getObjectPermissions(String apiKey) {
		//TODO: DATA-54 get perms for provided role
		Set<Permission> perms = new HashSet<>();
		return perms;
	}
	
	public Set<String> getStringPermissions(String apiKey) {
		//TODO: DATA-54 get perms for provided role
		Set<String> perms = new HashSet<>();
		return perms;
	}
}
