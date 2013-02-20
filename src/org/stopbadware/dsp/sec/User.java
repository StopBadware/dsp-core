package org.stopbadware.dsp.sec;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.shiro.authc.Account;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.permission.AllPermission;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.stopbadware.dsp.data.SecurityDBHandler;
import org.stopbadware.dsp.sec.RESTfulToken.Credentials;
import org.stopbadware.lib.util.SHA2;

public class User implements Account {

	private Credentials credentials;
	private SimplePrincipalCollection pc = new SimplePrincipalCollection();
	private SecurityDBHandler db = new SecurityDBHandler();
	
	private static final long serialVersionUID = -8172191017152833255L;

	public User(String principal) {
		pc.add(principal, AuthAuth.REALMNAME);
	}
	
	public User(String principal, Credentials credentials) {
		this(principal);
		String signature = SHA2.get256(principal+credentials.getTimestamp()+credentials.getPath()+db.getSecret(principal));
		this.credentials = new Credentials(signature, credentials.getPath(), credentials.getTimestamp());
	}
	
	@Override
	public Credentials getCredentials() {
		return credentials;
	}

	@Override
	public PrincipalCollection getPrincipals() {
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
