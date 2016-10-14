package de.mpg.imeji.rest.resources;

import static de.mpg.imeji.rest.process.RestProcessUtils.buildJSONResponse;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.mpg.imeji.rest.process.ProfileProcess;
import de.mpg.imeji.rest.to.JSONResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


@Path("/profiles")
@Api(value = "rest/profiles", description = "Operations on profiles")
public class ProfileResource implements ImejiResource {

  @Override
  @GET
  @ApiOperation(value = "Get all profiles user has access to.")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAll(@Context HttpServletRequest req, @QueryParam("q") String q,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    JSONResponse resp = ProfileProcess.readAll(req, q, offset, size);
    return buildJSONResponse(resp);
  }

  @Override
  @GET
  @Path("/{id}")
  @ApiOperation(
      value = "Get profile by id. If id is equal to \"default\", the default metadata profile will be delivered.")
  @Produces(MediaType.APPLICATION_JSON)
  public Response read(@Context HttpServletRequest req, @PathParam("id") String id) {
    JSONResponse resp = ProfileProcess.readProfile(req, id);
    return buildJSONResponse(resp);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Create Porifle or new version of profile",
      notes = "The body parameter is the json of a profile. You can get an example by using the get profile method.")
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context HttpServletRequest req, InputStream json) {
    JSONResponse resp = ProfileProcess.createProfile(req);
    return buildJSONResponse(resp);
  }

  @Override
  public Response create(@Context HttpServletRequest req) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Delete profile by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
    JSONResponse resp = ProfileProcess.deleteProfile(req, id);
    return buildJSONResponse(resp);
  }

  @PUT
  @Path("/{id}/release")
  @ApiOperation(value = "Release profile by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response release(@Context HttpServletRequest req, @PathParam("id") String id)
      throws Exception {
    JSONResponse resp = ProfileProcess.releaseProfile(req, id);
    return buildJSONResponse(resp);
  }

  @PUT
  @Path("/{id}/discard")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @ApiOperation(value = "Discard a metadata profile by id, with mandatory discard comment")
  @Produces(MediaType.APPLICATION_JSON)
  public Response withdraw(@Context HttpServletRequest req, @PathParam("id") String id,
      @FormParam("discardComment") String discardComment) throws Exception {
    JSONResponse resp = ProfileProcess.withdrawProfile(req, id, discardComment);
    return buildJSONResponse(resp);
  }


  @GET
  @Path("/{id}/template")
  @ApiOperation(value = "Get template item for a metadata profile")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readProfileItemTemplate(@Context HttpServletRequest req,
      @PathParam("id") String id) {
    JSONResponse resp = ProfileProcess.readItemTemplate(req, id);
    return buildJSONResponse(resp);
  }


}
