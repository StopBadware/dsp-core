package org.stopbadware.dsp.data;

import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authz.Permission;

public enum Role {
	
	ADMIN {
		public String toString() {
			return "Admin";
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
	
	public static Role castFromString(String role) {
		return null;
	}
	
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
				break;
			case DATA_IMPORTER:
				break;
			case IP_AS_RESOLVER:
				break;
			case DATA_SHARING_PARTICIPANT:
				break;
			default:
				break;
		}
		return perms;
	}

}
