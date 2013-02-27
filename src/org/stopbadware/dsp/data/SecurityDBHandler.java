package org.stopbadware.dsp.data;

import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

import org.apache.catalina.util.Base64;
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

/**
 * Class to handle all security related database events 
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
	
	private void test() {
		String clear = "yakabouche";
		String crypt = "";
		String decrypt = "";
		try {
//			Key key = KeyFactory.getInstance("DESede").generateKey(new PBEKeySpec(KEY.toCharArray()));
			SecretKey k = KeyGenerator.getInstance("AES").generateKey();
			byte[] key = k.getEncoded();
			SecretKey key2 = new SecretKeySpec(key, 0, key.length, "AES");
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, key2);
			byte[] encrypted = cipher.doFinal(clear.getBytes());
			crypt = Base64.encode(encrypted);
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (NoSuchPaddingException | InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalBlockSizeException | BadPaddingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println("clear:"+clear);
		System.out.println("crypt:"+crypt);
		System.out.println("clear:"+decrypt);
	}
	
	public SecurityDBHandler() {
		secdb = MongoDB.getSecurityDB();
		accountsColl = secdb.getCollection(MongoDB.ACCOUNTS);
		rolesColl = secdb.getCollection(MongoDB.ROLES);
	}
	
	public String getSecret(String apiKey) {
		test();	//DELME: DATA-54
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
	
	public String addUser(Set<String> roles) {
		String apiKey = createAPIKey();
		if (keyIsNotUnique(apiKey)) {
			/* unique key should be successfully generated on first attempt
			 * odds of same UUID + timestamp extremely low, if there is a
			 * collision, most likely symptom of something wrong */
			LOG.error("Generated API Key {} is already in use", apiKey);
			return null;
		}
		LOG.info("Unique API Key generated");
		//TODO: DATA-54 add user with roles as array 
		String secret = createSecret();
		if (secret != null) {
			String crypted = encryptSecret(secret);
			if (crypted != null && crypted.length() > 0) {
				//TODO: DATA-54 store crypted in db
			}
		}
		return apiKey;
	}
	
	private boolean keyIsNotUnique(String apiKey) {
		//TODO: DATA-54 verify key not already present
		return false;
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
