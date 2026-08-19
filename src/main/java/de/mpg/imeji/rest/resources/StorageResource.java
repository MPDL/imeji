package de.mpg.imeji.rest.resources;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.process.StorageProcess;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Created by vlad on 13.01.15.
 */
@Path("/storage")
@Tag(name = "storage")
public class StorageResource {

  @GET
  @Operation(summary = "Get storage properties")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStorageProperties() {
    return RestProcessUtils.buildJSONResponse(StorageProcess.getStorageProperties());
  }

  @GET
  @Path("/messages")
  @Operation(summary = "Get all messages")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getMessagess(@Context HttpServletRequest req) {
    return RestProcessUtils.buildJSONResponse(StorageProcess.getMessages(req));
  }
}
