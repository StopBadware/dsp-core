package org.stopbadware.dsp.rest;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.DBHandler;
import org.stopbadware.dsp.data.Permissions;
import org.stopbadware.dsp.data.SecurityDBHandler;
import org.stopbadware.dsp.json.SearchResults;
import org.stopbadware.dsp.json.TimeOfLast;
import org.stopbadware.dsp.sec.AuthAuth;

@Path("/events")
public class FindEventReports extends SecureREST {
	
	@GET
	@Path("/test")
	@Produces(MediaType.APPLICATION_JSON)
	public Object secTest() {	//DELME: DATA-54 auth test method
		DBHandler dbh = getDBH();
		SecurityDBHandler sec = new SecurityDBHandler();
		Set<String> roles = new HashSet<>();
		
		String key = sec.addUser(roles);
		System.out.println(key);
		if (subject.isAuthenticated() && subject.isPermitted("testperm")) {
			//TODO: DATA-54 do db stuff
//			System.out.println(subject.isPermitted(Permissions.TEST_PERM));
//			System.out.println(subject.isPermitted("foobar"));
			return new String("AOK");
		} else {
			//TODO: DATA-54 return 403
			System.out.println("(403) AUTH FAIL");
			return new String("403");
		}
	}
	
	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public SearchResults findSince(@PathParam("param") String sinceTime) {
		DBHandler dbh = new DBHandler(AuthAuth.getEmptySubject());	//TODO: DATA-50 fix call
		return dbh.testFind(Long.valueOf(sinceTime));				//TODO: DATA-53 replace
	}
	
	@GET
	@Path("/timeoflast/{source}")
	@Produces(MediaType.APPLICATION_JSON)
	public TimeOfLast getLastReportedTime(@PathParam("source") String source) {
		DBHandler dbh = new DBHandler(AuthAuth.getEmptySubject());	//TODO: DATA-50 fix call
		return dbh.getTimeOfLast(source);
	}
}
