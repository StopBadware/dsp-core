package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.registry.AlgorithmRegistry;
import org.jasypt.util.text.BasicTextEncryptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.sec.Role;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.BasicDBList;
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
	private static final String KEY = "foobar";
	private static final String ALGORITHM = "PBEWITHSHA1ANDDESEDE";
	private static StandardPBEStringEncryptor textEncryptor = new StandardPBEStringEncryptor();
	private static final Logger LOG = LoggerFactory.getLogger(SecurityDBHandler.class);
	public static final int ASC = MongoDB.ASC;
	public static final int DESC = MongoDB.DESC;
	
	static {
		textEncryptor.setAlgorithm(ALGORITHM);
		textEncryptor.setPassword(KEY);
	}
	
	public SecurityDBHandler() {
		secdb = MongoDB.getSecurityDB();
		accountsColl = secdb.getCollection(MongoDB.ACCOUNTS);
		rolesColl = secdb.getCollection(MongoDB.ROLES);
	}
	
	public String getSecret(String apiKey) {
		String crypted = "";	//TODO: DATA-54 get secret key from db
//		String decrypted = decryptSecret(crypted);
		
		String test = createSecret();	//DELME: DATA-54
		crypted = encryptSecret(test); 			//DELME: DATA-54
		String decrypted = decryptSecret(crypted);	//DELME: DATA-54
		System.out.println("cleartext:\t"+test);		//DELME: DATA-54
		System.out.println("ciphertext:\t"+crypted);	//DELME: DATA-54
		System.out.println("decrypted:\t"+decrypted);	//DELME: DATA-54
		return "SECRET";
	}
	
	public String addUser(Set<String> roles) {
		String apiKey = createAPIKey();
		//TODO: DATA-54 add user with roles as array 
		String secret = createSecret();
		if (secret != null) {
			String crypted = encryptSecret(secret);
			//TODO: DATA-54 store crypted in db
		}
		return apiKey;
	}
	
	private String createAPIKey() {
		//TODO: DATA-54 create API key
		return "";
	}
	
	private String createSecret() {
		StringBuilder secret = new StringBuilder("");
		secret.append(UUID.randomUUID().toString());
		secret.append("-" + System.currentTimeMillis());
		String hashed = SHA2.get256(secret.toString());
		return hashed;
	}
	
	private String encryptSecret(String cleartext) {
		return textEncryptor.encrypt(cleartext);
	}
	
	private String decryptSecret(String ciphertext) {
		return textEncryptor.decrypt(ciphertext);
	}
	
	public Set<String> getRoles(String apiKey) {
		//TODO: DATA-54 get roles from db
		System.out.println(apiKey);				//DELME: DATA-54
		Set<String> roles = new HashSet<>();
		DBObject query = new BasicDBObject("api_key", apiKey);
		DBCursor cur = accountsColl.find(query);
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			System.out.println(obj.toString());	//DELME: DATA-54
			if (obj.containsField("roles")) {
				BasicDBList roleList = (BasicDBList)obj.get("roles");
				for (Object role : roleList) {
					roles.add(role.toString());
					System.out.println(role.toString());	//DELME: DATA-54
				}
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
