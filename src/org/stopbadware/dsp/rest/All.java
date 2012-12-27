package org.stopbadware.dsp.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.stopbadware.dsp.data.Delme;

@Path("/all")
public class All {

	@GET
	@Path("/since/{param}")
	@Produces(MediaType.APPLICATION_JSON)
	public Delme test(@PathParam("param") String testString) {
		Delme d = new Delme();
		d.setA("all data");
		d.setB(testString);
		return d;
	}
}
