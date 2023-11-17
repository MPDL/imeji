package de.mpg.imeji.rest.resources;

import de.mpg.imeji.rest.process.ItemProcess;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static de.mpg.imeji.rest.process.ItemProcess.*;
import static de.mpg.imeji.rest.process.RestProcessUtils.buildJSONResponse;

@Path("/items")
@Tag(name = "items")
public class ItemResource implements ImejiResource {

  @Override
  @GET
  @Operation(summary = "Search and retrieve items")
  @Produces(MediaType.APPLICATION_JSON)
  public Response readAll(@Context HttpServletRequest req, @QueryParam("q") String q, @DefaultValue("0") @QueryParam("offset") int offset,
      @DefaultValue(DEFAULT_LIST_SIZE) @QueryParam("size") int size) {
    return buildJSONResponse(readItems(req, q, offset, size));
  }

  @Override
  @GET
  @Path("/{id}")
  @Operation(summary = "Get item by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response read(@Context HttpServletRequest req, @PathParam("id") String id) {
    return RestProcessUtils.buildJSONResponse(ItemProcess.readItem(req, id));
  }

  @POST
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Create new item with a File",
      responses = @ApiResponse(content = @Content(schema = @Schema(implementation = DefaultItemTO.class))),
      description = "Create an item with a file. File can be defined either as (by order of priority):"
          + "<div> 1) form parameter (multipart/form-data)</div>"
          + "<div> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) </div>"
          + "<div> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)</div>")
  @Produces(MediaType.APPLICATION_JSON)
  //@ApiImplicitParame({@ApiImplicitParam(name = "json", value = "json", required = true, dataType = "string",
  //   defaultValue = "{\"collectionId\":\"\"}", paramType = "form")})
  public Response create(@Context HttpServletRequest req,
      @Parameter(schema = @Schema(format = "binary", type = "string")) @FormDataParam("file") InputStream file,

      @Parameter(description = "File details", required = false,
          hidden = true) @FormDataParam("file") FormDataContentDisposition fileDetail,

      @Parameter(required = true,
          schema = @Schema(defaultValue = "{\"collectionId\":\"\"}", type = "string")) @FormDataParam(value = "json") String json) {
    final String origName = fileDetail != null ? fileDetail.getFileName() : null;
    return RestProcessUtils.buildJSONResponse(createItem(req, file, json, origName));
  }

  @PUT
  @Path("/{id}")
  @Consumes(MediaType.MULTIPART_FORM_DATA)
  @Operation(summary = "Update an item",
      responses = @ApiResponse(content = @Content(schema = @Schema(implementation = DefaultItemTO.class))),
      description = "Update an item with (optional) a file. File can be defined either as (by order of priority):"
          + "<div> 1) form parameter (multipart/form-data)</div>"
          + "<div> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) </div>"
          + "<div> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)</div>")
  @Produces(MediaType.APPLICATION_JSON)
  //@ApiImplicitParams({@ApiImplicitParam(name = "json", value = "json", required = true, dataType = "string",
  //    defaultValue = "{\"collectionId\":\"\"}", paramType = "form")})
  public Response update(@Context HttpServletRequest req,
      @Parameter(schema = @Schema(format = "binary", type = "string")) @FormDataParam("file") InputStream file,
      @Parameter(required = true,
          schema = @Schema(defaultValue = "{\"collectionId\":\"\"}", type = "string")) @FormDataParam("json") String json,
      @Parameter(description = "File details", required = false,
          hidden = true) @FormDataParam("file") FormDataContentDisposition fileDetail,
      @Parameter(required = true, description = "Item id") @PathParam("id") String id) {
    final String filename = fileDetail != null ? fileDetail.getFileName() : null;
    return RestProcessUtils.buildJSONResponse(updateItem(req, id, file, json, filename));
  }

  @Override
  public Response create(HttpServletRequest req) {
    return null;
  }

  @Override
  @DELETE
  @Path("/{id}")
  @Operation(summary = "Delete an item by id")
  @Produces(MediaType.APPLICATION_JSON)
  public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
    return RestProcessUtils.buildJSONResponse(deleteItem(req, id));
  }

}
