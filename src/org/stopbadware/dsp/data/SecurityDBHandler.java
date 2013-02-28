package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
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
import com.mongodb.WriteResult;

/**
 * Class to handle all security related database operations 
 */
public class SecurityDBHandler {
	
	private DB secdb;
	private DBCollection accountsColl;
	private DBCollection rolesColl;
	private static final String KEY = "La Forge Theta-2-9-9-7"; /* Geordi's command code from "The Mind's Eye" */
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
	
	/**
	 * Retrieves the secret key associated with the API key provided
	 * @param apiKey the API Key to retrieve the secret key for
	 * @return String containing the associated secret key, or an 
	 * empty String if one could not be retrieved for the provided API key
	 */
	public String getSecret(String apiKey) {
		String crypted = "";
		DBObject query = new BasicDBObject("api_key", apiKey);
		DBCursor cur = accountsColl.find(query).limit(1);
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj.containsField("secret_key")) {
				crypted = obj.get("secret_key").toString();
			}
		}
		return decryptSecret(crypted);
	}
	
	/**
	 * Adds a new user to the security database with the provided roles
	 * @param roles set of Roles to assign the new account
	 * @return a String representing the API Key for the new account, or
	 * null if an account could not be created
	 */
	public String addUser(Set<Role> roles) {
		String apiKey = createAPIKey();
		boolean userAdded = false;
		if (!keyIsUnique(apiKey)) {
			/* unique key should be successfully generated on first attempt
			 * odds of same UUID + timestamp extremely low, if there is a
			 * collision, most likely symptom of something wrong */
			LOG.error("Generated duplicate API Key:{}", apiKey);
			return null;
		}
		LOG.info("Unique API Key generated");
		
		String secret = createSecret();
		if (secret != null) {
			String crypted = encryptSecret(secret);
			if (crypted != null && crypted.length() > 0) {
				Set<String> roleStrings = new HashSet<>();
				for (Role role : roles) {
					roleStrings.add(role.toString());
				}
				userAdded = writeToDB(apiKey, crypted, roleStrings);
			}
		}
		 
		return (userAdded) ? apiKey : null;
	}
	
	private boolean writeToDB(String apiKey, String crypted, Set<String> roles) {
		boolean writeSuccess = false;
		DBObject account = new BasicDBObject();
		account.put("api_key", apiKey);
		account.put("secret_key", crypted);
		account.put("roles", roles.toArray(new String[roles.size()]));
		WriteResult wr = accountsColl.insert(account);
		if (wr.getError() != null) {
			LOG.error("Error writing API Key {}:\t{}", apiKey, wr.getError());
		} else {
			LOG.info("Wrote API Key {}", apiKey);
			writeSuccess = true;
		}
		return writeSuccess;
	}
	
	private boolean keyIsUnique(String apiKey) {
		DBObject query = new BasicDBObject("api_key", apiKey);
		int count = accountsColl.find(query).count();
		return count==0;
	}
	
	private String createAPIKey() {
		StringBuilder key = new StringBuilder("");
		key.append(UUID.randomUUID().toString());
		key.append("-"+Long.toString(System.currentTimeMillis(), 32));
		return key.toString();
	}
	
	private String createSecret() {
		StringBuilder secret = new StringBuilder("");
		secret.append(UUID.randomUUID().toString());
		secret.append("-" + System.currentTimeMillis());
		String hashed = SHA2.get256(secret.toString());
		return hashed;
	}
	
	private String encryptSecret(String cleartext) {
		String crypted = "";
		try {
			crypted = textEncryptor.encrypt(cleartext);
		} catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e) {
			LOG.error("Unable to encrypt provided cleartext '{}'\t{}", cleartext, e.getMessage());
		}
		return crypted;
	}
	
	private String decryptSecret(String ciphertext) {
		String clear = "";
		try {
			clear = textEncryptor.decrypt(ciphertext);
		} catch (EncryptionOperationNotPossibleException | EncryptionInitializationException e) {
			LOG.error("Unable to decrypt provided ciphertext '{}'\t{}", ciphertext, e.getMessage());
		}
		return clear;
	}
	
	/**
	 * Retrieves roles assigned to the API Key provided
	 * @param apiKey String of the API Key for the account
	 * @return a set of Strings representing the roles associated with the account
	 */
	public Set<String> getRoles(String apiKey) {
		Set<String> roles = new HashSet<>();
		DBObject query = new BasicDBObject("api_key", apiKey);
		DBCursor cur = accountsColl.find(query);
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj.containsField("roles")) {
				BasicDBList roleList = (BasicDBList)obj.get("roles");
				for (Object role : roleList) {
					roles.add(role.toString());
				}
			}
		}
		return roles;
	}
	
	/**
	 * Retrieves all permissions authorized to the API Key provided
	 * @param apiKey String of the API Key for the account
	 * @return a set of Shiro Permissions representing the all permissions
	 * associated with the account
	 */
	public Set<Permission> getObjectPermissions(String apiKey) {
		Set<Permission> perms = new HashSet<>();
		Set<String> roles = getRoles(apiKey);
		for (String role : roles) {
			try {
				perms.addAll(Role.fromString(role).getObjectPermissions());
			} catch (Exception e) {
				LOG.error("Error adding permissions for {} to object set:\t{}", role, e.getMessage());
			}
		}
		return perms;
	}
	
	/**
	 * Retrieves all permissions authorized to the API Key provided
	 * @param apiKey String of the API Key for the account
	 * @return a set of Strings representing the all permissions
	 * associated with the account
	 */
	public Set<String> getStringPermissions(String apiKey) {
		Set<String> perms = new HashSet<>();
		Set<String> roles = getRoles(apiKey);
		for (String role : roles) {
			try {
				perms.addAll(Role.fromString(role).getStringPermissions());
			} catch (Exception e) {
				LOG.error("Error adding permissions for {} to string set:\t{}", role, e.getMessage());
			}
		}
		return perms;
	}
	
	/**
	 * Retrieves all permissions authorized to the role
	 * @param role String representing the role to retrieve permissions for
	 * @return a set of Strings representing the all permissions
	 * associated with the role
	 */
	public Set<String> getRolePermissions(String role) {
		Set<String> perms = new HashSet<>();
		DBObject query = new BasicDBObject("role", role);
		DBCursor cur = rolesColl.find(query).limit(1);
		while (cur.hasNext()) {
			DBObject obj = cur.next();
			if (obj.containsField("permissions")) {
				BasicDBList permList = (BasicDBList)obj.get("permissions");
				for (Object perm : permList) {
					try {
						perms.add(perm.toString());
					} catch (Exception e) {
						LOG.error("Error adding permission {} to role set:\t{}", perm, e.getMessage());
					}
				}
			}
		}
		return perms;
	}
}
