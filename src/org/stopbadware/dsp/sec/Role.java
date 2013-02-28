package org.stopbadware.dsp.sec;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.stopbadware.dsp.data.SecurityDBHandler;

public enum Role {
	
	ADMIN,
	DATA_IMPORTER,
	IP_AS_RESOLVER,
	DATA_SHARING_PARTICIPANT,
	STOPBADWARE,
	TAKEDOWN_EXECUTER,
	RESEARCHER,
	PUBLIC,
	NO_PERMISSIONS;
	
	private Set<Permission> objPerms = new HashSet<>();
	private Set<String> strPerms = new HashSet<>();
	private static Map<String, Role> strMap = new HashMap<>();
	
	static {
		for (Role role : Role.values()) {
			strMap.put(role.toString(), role);
		}
		updateRolePermissions();
	}
	
	/**
	 * Gets the permissions associated with the Role
	 * @return Set of Shiro Permissions the associated role is authorized for
	 */
	public Set<Permission> getObjectPermissions() {
		return objPerms;
	}
	
	/**
	 * Gets the permissions associated with the Role
	 * @return Set of permissions as Strings the associated role is authorized for
	 */
	public Set<String> getStringPermissions() {
		return strPerms;
	}
	
	private void addPermission(String permission) {
		strPerms.add(permission);
		objPerms.add(new WildcardPermission(permission));
	}
	
	/**
	 * Updates each role with the permissions reflected in the datastore 
	 */
	public static void updateRolePermissions() {
		SecurityDBHandler db = new SecurityDBHandler();
		for (Role role : Role.values()) {
			Set<String> perms = db.getRolePermissions(role.toString());
			for (String perm : perms) {
				role.addPermission(perm);
			}
		}
	}
	
	/**
	 * Provides for safe casting of a string representation of a role
	 * to its associated Role enum
	 * @param role a case sensitive String representing a role
	 * @return a Role enum matching the provided string, or a Role with
	 * no authorized permissions if a match could not be found
	 */
	public static Role fromString(String role) {
		return (strMap.containsKey(role)) ? strMap.get(role) : NO_PERMISSIONS;
	}
	
}
