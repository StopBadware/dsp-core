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
	
	public Set<Permission> getPermissions(Role role) {
		Set<Permission> perms = new HashSet<>();
		
		return perms;
	}

}
