package de.mpg.imeji.rest.resources;

import static de.mpg.imeji.rest.process.ItemProcess.createItem;
import static de.mpg.imeji.rest.process.ItemProcess.deleteItem;
import static de.mpg.imeji.rest.process.ItemProcess.readItems;
import static de.mpg.imeji.rest.process.ItemProcess.updateItem;
import static de.mpg.imeji.rest.process.RestProcessUtils.buildJSONResponse;

import java.io.InputStream;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
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

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import de.mpg.imeji.rest.process.ItemProcess;
import de.mpg.imeji.rest.process.RestProcessUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;


@Path("/items")
@Api(value = "rest/items", description = "Operations on items")
public class ItemResource implements ImejiResource {


  @Override
  @GET
  @ApiOperation(value = "Search and retrieve items")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAll(@Context HttpServletRequest req, @QueryParam("q") String q,
      @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    return buildJSONResponse(readItems(req, q, offset, size));
  }

  @Override
  @GET
  @Path("/{id}")
  @ApiOperation(value = "Get item by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response read(@Context HttpServletRequest req, @PathParam("id") String id) {
    return RestProcessUtils.buildJSONResponse(ItemProcess.readItem(req, id));
  }


  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ApiOperation(value = "Create new item with a File",
      notes = "Create an item with a file. File can be defined either as (by order of priority):"
          + "<br/> 1) form parameter (multipart/form-data)<br/> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) "
          + "<br/> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)"
          + "<br/><br/>" + "You can get a json template by using"
          + "<br/><br/><a href=\"#!/collections/readCollectionItemTemplate\" target=\"_blank\">this method</a> "
          + "<br/><br/>or using the following basic json exmaple"
          + "<br/><br/><div class=\"json_example\">" + "{"
          + "<br/>\"collectionId\" : \"abc123\", (required)"
          + "<br/>\"fetchUrl\" : \"http://example.org/myFile.png\", (optional)"
          + "<br/>\"referenceUrl\" : \"http://example.org/myFile.png\", (optional)"
          + "<br/>\"filename\" : \"new filename\", (optional)" + "<br/>\"metadata\" : [] (optional)"
          + "<br/> }</div>")
  @Produces(MediaType.APPLICATION_JSON)
  public Response create(@Context HttpServletRequest req, @FormDataParam("file") InputStream file,
      @ApiParam(required = true) @FormDataParam("json") String json,
      @FormDataParam("file") FormDataContentDisposition fileDetail) {
    String origName = fileDetail != null ? fileDetail.getFileName() : null;
    return RestProcessUtils.buildJSONResponse(createItem(req, file, json, origName));
  }

  @PUT
  @Path("/{id}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @ApiOperation(value = "Update an item",
      notes = "Update an item with (optional) a file. File can be defined either as (by order of priority):"
          + "<br/> 1) form parameter (multipart/form-data)<br/> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) "
          + "<br/> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)"
          + "<br/><br/>" + "You can get a json template by using"
          + "<br/><br/><a href=\"#!/collections/readCollectionItemTemplate\" target=\"_blank\">this method</a> "
          + "<br/><br/>or using the following basic json exmaple"
          + "<br/><br/><div class=\"json_example\">" + "{"
          + "<br/>\"collectionId\" : \"abc123\", (required)"
          + "<br/>\"fetchUrl\" : \"http://example.org/myFile.png\", (optional)"
          + "<br/>\"referenceUrl\" : \"http://example.org/myFile.png\", (optional)"
          + "<br/>\"filename\" : \"new filename\", (optional)" + "<br/>\"metadata\" : [] (optional)"
          + "<br/> }</div>")
  @Produces(MediaType.APPLICATION_JSON)
  public Response update(@Context HttpServletRequest req, @FormDataParam("file") InputStream file,
      @ApiParam(required = true) @FormDataParam("json") String json,
      @FormDataParam("file") FormDataContentDisposition fileDetail, @PathParam("id") String id) {
    String filename = fileDetail != null ? fileDetail.getFileName() : null;
    return RestProcessUtils.buildJSONResponse(updateItem(req, id, file, json, filename));
  }

  @Override
  public Response create(HttpServletRequest req) {
    return null;
  }

  @Override
  @DELETE
  @Path("/{id}")
  @ApiOperation(value = "Delete an item by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
    return RestProcessUtils.buildJSONResponse(deleteItem(req, id));
  }


}
