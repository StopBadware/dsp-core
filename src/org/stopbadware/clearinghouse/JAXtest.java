package org.stopbadware.clearinghouse;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/jax")
public class JAXtest {

	@GET
	@Path("/{param}")
	@Produces(MediaType.TEXT_PLAIN)
	public Response test(@PathParam("param") String testString) {
		String response = ">>" + testString + "<<";
		return Response.status(200).entity(response).build();
	}
}
