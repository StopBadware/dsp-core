package org.stopbadware.dsp.sec;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.Account;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.stopbadware.dsp.sec.RESTfulToken.Credentials;

public class User implements Account {

	private static final long serialVersionUID = -8172191017152833255L;

	public User(String principal) {
		System.out.println(">>"+principal+"<<");
	}
	
	public User(String principal, Credentials credentials) {
		System.out.println(">>"+principal+"<<\t"+credentials.getSignature());
//		System.out.println(">>"+principal+"<<\t");
	}
	
	@Override
	public Object getCredentials() {
		//TODO: DATA-54 implement
		String sig = "1fea95532ec9ad15a32a1c513a28f453aee027c56df8bff62b324ada5cb5c72b";
		return new RESTfulToken.Credentials(sig, 1360786590L);
	}

	@Override
	public PrincipalCollection getPrincipals() {
		//TODO: DATA-54 implement
		SimplePrincipalCollection pc = new SimplePrincipalCollection();
		pc.add("DATA123456", "SBW-DSP");
		return pc;
	}

	@Override
	public Collection<Permission> getObjectPermissions() {
		//TODO: DATA-54 implement
		Set<Permission> perms = new HashSet<>();
		perms.add(new AllPermission());
		return perms;
	}

	@Override
	public Collection<String> getRoles() {
		//TODO: DATA-54 implement
		Set<String> roles = new HashSet<>();
		roles.add("testrole");
		return roles;
	}

	@Override
	public Collection<String> getStringPermissions() {
		//TODO: DATA-54 implement
		Set<String> perms = new HashSet<>();
		perms.add("testperm");
		return perms;
	}

}
