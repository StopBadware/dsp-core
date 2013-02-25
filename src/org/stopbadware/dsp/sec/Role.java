package org.stopbadware.dsp.sec;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.WildcardPermission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.stopbadware.dsp.data.SecurityDBHandler;

public enum Role {
	
	ADMIN,
	DATA_IMPORTER,
	IP_AS_RESOLVER,
	DATA_SHARING_PARTICIPANT;
	
	private Set<Permission> objPerms = new HashSet<>();
	private Set<String> strPerms = new HashSet<>();
	private static final Logger LOG = LoggerFactory.getLogger(Role.class);
	
	static {
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
		System.out.println("updating perms");		//DELME: DATA-54
		//TODO: DATA-54 grab perms from db and update roles
		for (Role role : Role.values()) {
			System.out.println(role.toString());	//DELME: DATA-54
			Set<String> perms = db.getPermissions(role.toString());
			for (String perm : perms) {
				System.out.println(perm);	//DELME: DATA-54
				role.addPermission(perm);
			}
		}
		System.out.println(ADMIN.getObjectPermissions().size());
		System.out.println(ADMIN.getStringPermissions().size());
	}
	
	public static Role castFromString(String role) {
		//TODO: DATA-54 add casting
		return ADMIN;
	}
	
//	//TODO: DATA-54 replace with get string perms
//	public static Set<Permission> getPermissions(String role) {
//		Role r = castFromString(role);
//		if (r != null) {
//			System.out.println("not null");
//			return getPermissions(r);
//		} else {
//			System.out.println("null");
//			return new HashSet<>();
//		}
//	}
//	
//	public static Set<Permission> getPermissions(Role role) {
//		Set<Permission> perms = new HashSet<>();
//		switch(role) {
//			case ADMIN:
//				//TODO: DATA-54 add admin perms
//				break;
//			case DATA_IMPORTER:
//				//TODO: DATA-54 add importer perms
//				break;
//			case IP_AS_RESOLVER:
//				//TODO: DATA-54 add resolver perms
//				break;
//			case DATA_SHARING_PARTICIPANT:
//				//TODO: DATA-54 add participant perms
//				break;
//			default:
//				break;
//		}
//		return perms;
//	}

}
