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
	NONE;
	
	private Set<Permission> objPerms = new HashSet<>();
	private Set<String> strPerms = new HashSet<>();
	private static Map<String, Role> strMap = new HashMap<>();
	
	static {
		for (Role role : Role.values()) {
			strMap.put(role.toString(), role);
		}
		updateRolePermissions();
	}
	
	public Set<Permission> getObjectPermissions() {
		return objPerms;
	}
	
	public Set<String> getStringPermissions() {
		return strPerms;
	}
	
	private void addPermission(String permission) {
		strPerms.add(permission);
		objPerms.add(new WildcardPermission(permission));
	}
	
	public static void updateRolePermissions() {
		SecurityDBHandler db = new SecurityDBHandler();
		for (Role role : Role.values()) {
			Set<String> perms = db.getPermissions(role.toString());
			for (String perm : perms) {
				role.addPermission(perm);
			}
		}
	}
	
	public static Role fromString(String role) {
		return (strMap.containsKey(role)) ? strMap.get(role) : NONE;
	}
	
}
