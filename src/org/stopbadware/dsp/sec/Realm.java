package org.stopbadware.dsp.sec;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.stopbadware.dsp.sec.RESTfulToken.Credentials;

public class Realm extends AuthorizingRealm {

	public Realm() {
		this.setAuthenticationTokenClass(RESTfulToken.class);
		this.setName(AuthAuth.REALMNAME);
	}
	
	@Override
	protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
		Object obj = principals.iterator().next();
		return new User(obj.toString());
	}

	@Override
	protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken arg0) throws AuthenticationException {
		String principal = null;
		Credentials credentials = null;
		if (arg0 instanceof RESTfulToken) {
			RESTfulToken token = (RESTfulToken) arg0;
			principal = token.getPrincipal();
			credentials = token.getCredentials();
		} else {
			principal = "";
			credentials = new Credentials("", "", 0L); 
		}
		return new User(principal, credentials);
	}
	
}
