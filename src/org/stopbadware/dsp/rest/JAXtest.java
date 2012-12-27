package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.stopbadware.dsp.data.Delme;

@Path("/jax")
public class JAXtest {

	@GET
	@Path("/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Delme test(@PathParam("param") String testString) {
//		String response = ">>" + testString + "<<";
//		return Response.status(200).entity(response).build();
		Delme d = new Delme();
		d.setA("foo");
		d.setB(testString);
		return d;
	}
}
