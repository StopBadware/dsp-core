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
	private SecurityDBHandler secdb = new SecurityDBHandler();
	
	private static final long serialVersionUID = -8172191017152833255L;

	public User(String principal) {
		pc.add(principal, AuthAuth.REALMNAME);
	}
	
	public User(String principal, Credentials credentials) {
		this(principal);
		String signature = SHA2.get256(principal+credentials.getTimestamp()+credentials.getPath()+secdb.getSecret(principal));
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
	public Collection<String> getRoles() {
		return secdb.getRoles(pc.getPrimaryPrincipal().toString());
	}
	
	@Override
	public Collection<Permission> getObjectPermissions() {
		return secdb.getObjectPermissions(pc.getPrimaryPrincipal().toString());
	}

	@Override
	public Collection<String> getStringPermissions() {
		return secdb.getStringPermissions(pc.getPrimaryPrincipal().toString());
	}

}
