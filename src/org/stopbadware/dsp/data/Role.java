package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Role {
	
	ADMIN {
		private Set<Permission> objPerms = new HashSet<>();
		private Set<String> strPerms = new HashSet<>();
		public String toString() {
			return "Admin";
		}
		public Set<Permission> getObjectPermissions() {
			return objPerms;
		}
		public Set<String> getStringPermissions() {
			return strPerms;
		}
	},
	DATA_IMPORTER {
		public String toString() {
			return "Importer";
		}
	},
	IP_AS_RESOLVER {
		public String toString() {
			return "Resolver";
		}
	},
	DATA_SHARING_PARTICIPANT {
		public String toString() {
			return "Participant";
		}
	};
	
	private static final Logger LOG = LoggerFactory.getLogger(Role.class);
	
	static {
		//TODO: DATA-54 grab perms from db and call updateroleperms
		boolean updated = updateRolePermissions();
		if (updated) {
			LOG.info("Successfully upated Role permissions from database");
		} else {
			LOG.error("Failed updating Role permissions from database");
		}
	}
	
	public static boolean updateRolePermissions() {
		System.out.println("updating perms");	//DELME: DATA-54
		//TODO: DATA-54 grab perms from db and update roles
		return true;
	}
	
	public static Role castFromString(String role) {
		//TODO: DATA-54 add casting
		return null;
	}
	
	//TODO: DATA-54 replace with get string perms
	public static Set<Permission> getPermissions(String role) {
		Role r = castFromString(role);
		if (r != null) {
			System.out.println("not null");
			return getPermissions(r);
		} else {
			System.out.println("null");
			return new HashSet<>();
		}
	}
	
	public static Set<Permission> getPermissions(Role role) {
		Set<Permission> perms = new HashSet<>();
		switch(role) {
			case ADMIN:
				//TODO: DATA-54 add admin perms
				break;
			case DATA_IMPORTER:
				//TODO: DATA-54 add importer perms
				break;
			case IP_AS_RESOLVER:
				//TODO: DATA-54 add resolver perms
				break;
			case DATA_SHARING_PARTICIPANT:
				//TODO: DATA-54 add participant perms
				break;
			default:
				break;
		}
		return perms;
	}

}
