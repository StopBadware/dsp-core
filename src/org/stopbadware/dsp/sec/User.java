package org.stopbadware.dsp.sec;

import java.util.Collection;

import org.apache.shiro.authc.Account;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.subject.PrincipalCollection;

public class User implements Account {

	private static final long serialVersionUID = -8172191017152833255L;

	public User() {
		
	}
	
	@Override
	public Object getCredentials() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public PrincipalCollection getPrincipals() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Permission> getObjectPermissions() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getRoles() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<String> getStringPermissions() {
		// TODO Auto-generated method stub
		return null;
	}

}
