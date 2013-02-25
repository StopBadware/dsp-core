package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.sec.Role;

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
	
	public String addUser(Set<String> roles) {
		//TODO: DATA-54 create API key 
		//TODO: DATA-54 add user with roles as array 
		//TODO: DATA-54 create secret
		//TODO: DATA-54 store secret encrypted with padding
		//TODO: DATA-54 return apikey
		return "SECRET";
	}
	
	private String encryptSecret(String plaintext) {
		return "";	//TODO: DATA-54 pad and encrypt
	}
	
	private String decryptSecret(String secret) {
		return "";	//TODO: DATA-54 decrypt and remove padding
	}
	
	public Set<String> getRoles(String apiKey) {
		//TODO: DATA-54 get roles from db
		Set<String> roles = new HashSet<>();
		return roles;
	}
	
	//TODO: DATA-54 write permissions for roles in db
	public Set<Permission> getObjectPermissions(String apiKey) {
		//TODO: DATA-54 get perms for all roles of provided key
		Set<Permission> perms = new HashSet<>();
		Role.castFromString("rolefromdb").getObjectPermissions();
		//TODO: DATA-54 addall perms
		return perms;
	}
	
	public Set<String> getStringPermissions(String apiKey) {
		//TODO: DATA-54 get perms for all roles of provided key
		Set<String> perms = new HashSet<>();
		Role.castFromString("rolefromdb").getStringPermissions();
		//TODO: DATA-54 addall perms
		return perms;
	}
	
	public Set<String> getPermissions(String role) {
		Set<String> perms = new HashSet<>();
		//TODO: DATA-54 get perms from db for provided role
		perms.add("foo");
		perms.add("bar");
		return perms;
	}
}
