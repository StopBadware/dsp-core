package org.stopbadware.dsp.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.data.SecurityDBHandler;
import org.stopbadware.dsp.json.AccountInfo;
import org.stopbadware.dsp.json.Response;
import org.stopbadware.dsp.json.Simple;
import org.stopbadware.dsp.sec.Role;
import org.stopbadware.dsp.json.Error;

@Path("/admin")
public class Admin extends SecureREST {

	@POST
	@Path("/account/create/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response createNewAccount(@PathParam("param") String prefix) {
		Response response = null;
		DBHandler dbh = getDBH();
		if (dbh != null) {
			SecurityDBHandler secdb = new SecurityDBHandler();
			Set<Role> roles = new HashSet<>();
			roles.add(Role.DATA_SHARING_PARTICIPANT);
			String apiKey = secdb.addUser(roles, getSubject(), prefix);
			String secret = (apiKey != null) ? secdb.getSecret(apiKey) : null;
			
			if (apiKey != null && secret != null) {
				response = new AccountInfo(apiKey, secret);
			} else {
				response = new Error(Error.REQUEST_FAILED, "Unable to create account");
			}
		} else {
			response = httpResponseCode(FORBIDDEN);
		}
		return response;
	}
	
	@POST
	@Path("/account/disable/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response disableAccount(@PathParam("param") String pubKey) {
		Response response = null;
		DBHandler dbh = getDBH();
		if (dbh != null) {
			SecurityDBHandler secdb = new SecurityDBHandler();
			boolean disabled = secdb.disableUser(pubKey, getSubject());
			if (disabled) {
				response = new Simple("Account "+pubKey+" has been disabled");
			} else {
				response = new Error(Error.REQUEST_FAILED, "Unable to disable account");
			}
		} else {
			response = httpResponseCode(FORBIDDEN);
		}
		return response;
	}
}
