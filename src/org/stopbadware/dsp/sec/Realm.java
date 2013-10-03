package org.stopbadware.dsp.sec;

import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.stopbadware.dsp.sec.RestToken.Credentials;

public class Realm extends AuthorizingRealm {

	public static String REALMNAME = "SBW-DSP";
	
	public Realm() {
		this.setAuthenticationTokenClass(RestToken.class);
		this.setName(REALMNAME);
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
		if (arg0 instanceof RestToken) {
			RestToken token = (RestToken) arg0;
			principal = token.getPrincipal();
			credentials = token.getCredentials();
		} else {
			principal = "";
			credentials = new Credentials("", "", 0L); 
		}
		return new User(principal, credentials);
	}
	
}
