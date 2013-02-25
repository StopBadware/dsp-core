package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.sec.Role;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;

/**
 * Class to handle all security related database events 
 */
public class SecurityDBHandler {
	
	private DB secdb;
	private DBCollection accountsColl;
	private DBCollection rolesColl;
	private static final Logger LOG = LoggerFactory.getLogger(SecurityDBHandler.class);
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;
	
	public SecurityDBHandler() {
		secdb = MongoDB.getSecurityDB();
		accountsColl = secdb.getCollection(MongoDB.ACCOUNTS);
		rolesColl = secdb.getCollection(MongoDB.ROLES);
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
		DBObject query = new BasicDBObject("api_key", apiKey);
		DBCursor cur = accountsColl.find(query);
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			System.out.println(obj.toString());	//DELME: DATA-54
			if (obj.containsField("roles")) {
				obj.get("roles");	//TODO: DATA-54 verify array?
			}
		}
		return roles;
	}
	
	//TODO: DATA-54 write permissions for roles in db
	
	public Set<Permission> getObjectPermissions(String apiKey) {
		//TODO: DATA-54 get perms for all roles of provided key
		Set<Permission> perms = new HashSet<>();
		Role.fromString("rolefromdb").getObjectPermissions();
		//TODO: DATA-54 addall perms
		return perms;
	}
	
	public Set<String> getStringPermissions(String apiKey) {
		//TODO: DATA-54 get perms for all roles of provided key
		Set<String> perms = new HashSet<>();
		Role.fromString("rolefromdb").getStringPermissions();
		//TODO: DATA-54 addall perms
		return perms;
	}
	
	public Set<String> getPermissions(String role) {
		Set<String> perms = new HashSet<>();
		//TODO: DATA-54 get perms from db for provided role
		perms.add("foo");	//DELME: DATA-54
		perms.add("bar");	//DELME: DATA-54
		return perms;
	}
}
