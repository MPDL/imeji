package de.mpg.imeji.rest.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.process.StorageProcess;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Created by vlad on 13.01.15.
 */
@Path("/storage")
@Api(value = "storage")
public class StorageResource {

	@GET
	@ApiOperation(value = "Get storage properties")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getStorageProperties() {
		return RestProcessUtils.buildJSONResponse(StorageProcess.getStorageProperties());
	}

	@GET
	@Path("/messages")
	@ApiOperation(value = "Get all messages")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getMessagess(@Context HttpServletRequest req) {
		return RestProcessUtils.buildJSONResponse(StorageProcess.getMessages(req));
	}
}
