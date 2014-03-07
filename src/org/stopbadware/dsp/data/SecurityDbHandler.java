package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.Subject;
import org.jasypt.encryption.pbe.StandardPBEStringEncryptor;
import org.jasypt.exceptions.EncryptionInitializationException;
import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.sec.Permissions;
import org.stopbadware.dsp.sec.Role;
import org.stopbadware.lib.util.SHA2;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.mongodb.WriteResult;

/**
 * Class to handle all security related database operations 
 */
public class SecurityDbHandler {
	
	private DB db;
	private DBCollection accountsColl;
	private DBCollection rolesColl;
	private static final String KEY = System.getenv("CRYPT_KEY");
	private static final String ALGORITHM = System.getenv("CRYPT_ALG");
	private static final StandardPBEStringEncryptor textEncryptor = new StandardPBEStringEncryptor();
	private static final int RATE_LIMIT_MAX = Integer.valueOf(System.getenv("RATE_LIMIT_MAX"));
	public static final long RATE_LIMIT_SECS = Long.valueOf(System.getenv("RATE_LIMIT_SECONDS"));
	private static final Logger LOG = LoggerFactory.getLogger(SecurityDbHandler.class);
	public static final int ASC = MongoDb.ASC;
	public static final int DESC = MongoDb.DESC;
	
	static {
		textEncryptor.setAlgorithm(ALGORITHM);
		textEncryptor.setPassword(KEY);
	}
	
	public SecurityDbHandler() {
		db = MongoDb.getDB();
		accountsColl = db.getCollection(MongoDb.ACCOUNTS);
		rolesColl = db.getCollection(MongoDb.ROLES);
	}
	
	/**
	 * Retrieves the secret key associated with the API key provided
	 * @param apiKey the API Key to retrieve the secret key for
	 * @return String containing the associated secret key, an empty 
	 * string is returned if one could not be retrieved for the provided API key
	 * (i.e. key doesn't exist or account is disabled)
	 */
	public String getSecret(String apiKey) {
		String crypted = "";
		boolean enabled = false;
		int attempt = 0;
		final int maxAttempts = 3;
		/* [DATA-129] MongoDB's Java driver only detects dead connection on use (throwing a SocketException)
		 * making multiple attempts on a request's first DB access (retrieving the secret key during
		 * authentication) to handle dead connections */
		while (crypted.isEmpty() && attempt < maxAttempts) {
			try {
				DBObject account = getAccount(apiKey);
				if (account != null) {
					if (account.containsField("secret_key")) {
						crypted = account.get("secret_key").toString();
					}
					if (account.containsField("enabled")) {
						enabled = account.get("enabled").toString().equalsIgnoreCase("true");
					}
				}
				touch(apiKey);
			} catch (Exception e) {
				if (attempt > 0) {
					LOG.warn("Exception thrown while retrieving key for '{}': {}", apiKey, e.getMessage());
				}
			}
			attempt++;
		}
		if (attempt >= maxAttempts && crypted.isEmpty()) {
			LOG.error("Failed to retrieve key for '{}'", apiKey);
		}
		return (enabled && crypted.length() > 0) ? decryptSecret(crypted) : "";
	}
	
	/**
	 * Updates the account's access fields (last access time, number of access attempts)
	 * @param apiKey API Key of the account to update
	 */
	private void touch(String apiKey) {
		DBObject doc = new BasicDBObject("api_key", apiKey);
		DBObject upd = new BasicDBObject();
		upd.put("$inc", new BasicDBObject("num_access", 1));
		upd.put("$set", new BasicDBObject("last_access", System.currentTimeMillis()/1000));
		WriteResult wr = null;
		try {
			wr = accountsColl.update(doc, upd);
			if (wr.getError() != null) {
				LOG.error("Unable to update access fields:\t{}", wr.getError());
			}
		} catch (MongoException e) {
			LOG.error("Unable to update access fields:\t{}", e.getMessage());
		}
	}
	
	/**
	 * Updates the account's rate limit fields
	 * @param apiKey API Key of the account to update
	 * @param reset if this update should reset the counter to zero (true) or
	 * increment the counter by one (false)
	 */
	private void touchRateLimit(String apiKey, boolean reset) {
		DBObject doc = new BasicDBObject("api_key", apiKey);
		DBObject upd = new BasicDBObject();
		DBObject set = new BasicDBObject();
		if (reset) {
			set.put("rate_limit_num_access", 0);
		} else {
			upd.put("$inc", new BasicDBObject("rate_limit_num_access", 1));
		}
		long resetTime = (System.currentTimeMillis() / 1000) + RATE_LIMIT_SECS;
		set.put("rate_limit_reset_time", resetTime);
		upd.put("$set", set);
		WriteResult wr = null;
		try {
			wr = accountsColl.update(doc, upd);
			if (wr.getError() != null) {
				LOG.error("Unable to update access fields:\t{}", wr.getError());
			}
		} catch (MongoException e) {
			LOG.error("Unable to update access fields:\t{}", e.getMessage());
		}
	}
	
	/**
	 * Checks if an account is rate limited
	 * @param apiKey API key of the account to check
	 * @return true if the account has accessed rate limited resources more than the 
	 * RATE_LIMIT_MAX setting times in the previous RATE_LIMIT_SECONDS setting seconds
	 */
	public boolean isRateLimited(String apiKey) {
		boolean rateLimited = true;
		DBObject account = getAccount(apiKey);
		if (account != null) {
			long resetTime; 
			try {
				resetTime = Long.valueOf(account.get("rate_limit_reset_time").toString());
			} catch (NumberFormatException | NullPointerException e) {
				resetTime = 0L;
			}
			if (resetTime < System.currentTimeMillis() / 1000) {
				touchRateLimit(apiKey, true);
				rateLimited = false;
			} else {
				touchRateLimit(apiKey, false);
				int ctr;
				try {
					ctr = Integer.valueOf(account.get("rate_limit_num_access").toString());
				} catch (NumberFormatException | NullPointerException e) {
					ctr = 1;
				}
				rateLimited = ctr >= RATE_LIMIT_MAX;
			}
			if (rateLimited) {
				LOG.warn("{} exceeded rate limit ({} requests over {} seconds)", apiKey, RATE_LIMIT_MAX, RATE_LIMIT_SECS);
			}
		}
		return rateLimited;
	}
	
	/**
	 * Disables an API account
	 * @param publicKey the public API key of the account to disable
	 * @param subject a Shiro subject with sufficient authorization to disable users
	 * @return true if the account was successfully disabled
	 */
	public boolean disableUser(String publicKey, Subject subject) {
		boolean disabled = false; 
		DBObject doc = new BasicDBObject("api_key", publicKey);
		DBObject upd = new BasicDBObject("$set", new BasicDBObject("enabled", false));
		WriteResult wr = null;
		try {
			if (subject.isPermitted(Permissions.WRITE_ACCOUNTS)) {
				wr = accountsColl.update(doc, upd);
				if (wr.getError() != null) {
					LOG.error("Unable to disable account '{}':\t{}", publicKey, wr.getError());
				} else {
					disabled = true;
					LOG.info("Account '{}' has been disabled", publicKey);
				}
			} else {
				LOG.warn("{} NOT authorized for {}", subject.getPrincipal(), Permissions.WRITE_ACCOUNTS);
			}
		} catch (MongoException e) {
			LOG.error("Unable to disable account '{}':\t{}", publicKey, e.getMessage());
		}
		return disabled;
	}
	
	/**
	 * Adds a new user to the security database with the provided roles
	 * @param roles set of Roles to assign the new account
	 * @param subject a Shiro subject with sufficient authorization to add
	 * new users
	 * @param participantPrefix the prefix of the organization associated with this key
	 * @return a String representing the API Key for the new account, or
	 * null if an account could not be created
	 */
	public String addUser(Set<Role> roles, String participantPrefix, Subject subject) {
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
				userAdded = writeAccount(apiKey, crypted, roleStrings, subject, participantPrefix);
			}
		}
		 
		return (userAdded) ? apiKey : null;
	}
	
	private boolean writeAccount(String apiKey, String crypted, Set<String> roles, Subject subject, String participantPrefix) {
		boolean writeSuccess = false;
		DBObject account = new BasicDBObject();
		account.put("api_key", apiKey);
		account.put("secret_key", crypted);
		account.put("roles", roles.toArray(new String[roles.size()]));
		account.put("participant", participantPrefix);
		account.put("enabled", true);
		account.put("last_access", 0);
		account.put("num_access", 0);
		WriteResult wr = null;
		if (subject.isPermitted(Permissions.WRITE_ACCOUNTS)) {
			wr = accountsColl.insert(account);
		}
		if (wr != null) {
			if (wr.getError() != null) {
				LOG.error("Error writing API Key {}:\t{}", apiKey, wr.getError());
			} else {
				LOG.info("Wrote API Key {}", apiKey);
				writeSuccess = true;
			}
		} else {
			LOG.warn("{} NOT authorized for {}", subject.getPrincipal(), Permissions.WRITE_ACCOUNTS);
		}
		return writeSuccess;
	}
	
	private boolean keyIsUnique(String apiKey) {
		return getAccount(apiKey) == null;
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
	 * Retrieves the participant's prefix associated with the API key provided
	 * @param apiKey the public API key to check
	 * @return String containing the particpant's prefix, or an empty String if
	 * no participant is associated with the API key
	 */
	public String getParticipant(String apiKey) {
		String participant = "";
		DBObject account = getAccount(apiKey);
		if (account != null && account.containsField("participant")) {
			participant = account.get("participant").toString();
		}
		return participant;
	}
	
	/**
	 * Retrieves roles assigned to the API Key provided
	 * @param apiKey String of the API Key for the account
	 * @return a set of Strings representing the roles associated with the account
	 */
	public Set<String> getRoles(String apiKey) {
		Set<String> roles = new HashSet<>();
		DBObject account = getAccount(apiKey);
		if (account != null) {
			if (account.containsField("roles")) {
				BasicDBList roleList = (BasicDBList)account.get("roles");
				for (Object role : roleList) {
					roles.add(role.toString());
				}
			}
		}
		return roles;
	}
	
	private DBObject getAccount(String apiKey) {
		return accountsColl.findOne(new BasicDBObject("api_key", apiKey));
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
	 * @return a set of Strings representing the permissions
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
