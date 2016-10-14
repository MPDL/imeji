package de.mpg.imeji.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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
@Api(value = "rest/storage", description = "Storage properties")
public class StorageResource {

  @GET
  @ApiOperation(value = "Get storage properties")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getStorageProperties() {
    return RestProcessUtils.buildJSONResponse(StorageProcess.getStorageProperties());
  }

}
