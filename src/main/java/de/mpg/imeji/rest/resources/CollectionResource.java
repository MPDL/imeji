package de.mpg.imeji.rest.resources;

import static de.mpg.imeji.rest.process.CollectionProcess.createCollection;
import static de.mpg.imeji.rest.process.CollectionProcess.deleteCollection;
import static de.mpg.imeji.rest.process.CollectionProcess.getCollectionElements;
import static de.mpg.imeji.rest.process.CollectionProcess.readAllCollections;
import static de.mpg.imeji.rest.process.CollectionProcess.readCollection;
import static de.mpg.imeji.rest.process.CollectionProcess.readCollectionItems;
import static de.mpg.imeji.rest.process.CollectionProcess.releaseCollection;
import static de.mpg.imeji.rest.process.CollectionProcess.updateCollection;
import static de.mpg.imeji.rest.process.CollectionProcess.withdrawCollection;
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

import de.mpg.imeji.rest.to.JSONResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;


// @Singleton
@Path("/collections")
@Tag(name = "collections")
public class CollectionResource implements ImejiResource {

  @GET
  @Path("/{id}/items")
  @Operation(summary = "Search and retrieve items of the collection")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readItemsWithQuery(@Context HttpServletRequest req, @PathParam("id") String id, @QueryParam("q") String q,
      @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    final JSONResponse resp = readCollectionItems(req, id, q, offset, size);
    return buildJSONResponse(resp);
  }

  @GET
  @Path("/{id}/elements")
  @Operation(summary = "Search and retrieve items and subcollection of the collection")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readContentsWithQuery(@Context HttpServletRequest req, @PathParam("id") String id, @QueryParam("q") String q,
      @DefaultValue("0") @QueryParam("offset") int offset, @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    final JSONResponse resp = getCollectionElements(req, id, q, offset, size, null, null);
    return buildJSONResponse(resp);
  }

  @Override
  @GET
  @Operation(summary = "Get all collections user has access to.",
      description = "With provided query parameter you filter only some collections")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAll(@Context HttpServletRequest req, @QueryParam("q") String q, @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    final JSONResponse resp = readAllCollections(req, q, offset, size);
    return buildJSONResponse(resp);
  }

  @Override
  @GET
  @Path("/{id}")
  @Operation(summary = "Get collection by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response read(@Context HttpServletRequest req, @PathParam("id") String id) {
    final JSONResponse resp = readCollection(req, id);
    return buildJSONResponse(resp);
  }

  @PUT
  @Path("/{id}")
  @Operation(summary = "Update collection by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(@Context HttpServletRequest req, InputStream json, @PathParam("id") String id) throws Exception {
    final JSONResponse resp = updateCollection(req, id);
    return buildJSONResponse(resp);
  }

  @PUT
  @Path("/{id}/release")
  @Operation(summary = "Release collection by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response release(@Context HttpServletRequest req, @PathParam("id") String id) throws Exception {
    final JSONResponse resp = releaseCollection(req, id);
    return buildJSONResponse(resp);
  }

  @PUT
  @Path("/{id}/discard")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  @Operation(summary = "Discard a collection by id, with mandatory discard comment")
  @Produces(MediaType.APPLICATION_JSON)
  public Response withdraw(@Context HttpServletRequest req, @PathParam("id") String id, @FormParam("discardComment") String discardComment)
      throws Exception {
    final JSONResponse resp = withdrawCollection(req, id, discardComment);
    return buildJSONResponse(resp);
  }

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Create collection or new version of collection",
      description = "The body parameter is the json of a collection. You can get an example by using the get collection method.")
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context HttpServletRequest req, InputStream json) {
    final JSONResponse resp = createCollection(req);
    return buildJSONResponse(resp);
  }

  @Override
  @DELETE
  @Path("/{id}")
  @Operation(summary = "Delete collection by id", description = "Deletes also items of this collection")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
    final JSONResponse resp = deleteCollection(req, id);
    return buildJSONResponse(resp);
  }

  @Override
  public Response create(HttpServletRequest req) {
    // TODO Auto-generated method stub
    return null;
  }
}
