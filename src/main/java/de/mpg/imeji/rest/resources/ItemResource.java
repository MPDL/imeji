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
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Path("/items")
@Api(value = "items")
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
	@ApiOperation(value = "Create new item with a File", response = DefaultItemTO.class, notes = "Create an item with a file. File can be defined either as (by order of priority):"
			+ "<div> 1) form parameter (multipart/form-data)</div>"
			+ "<div> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) </div>"
			+ "<div> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)</div>")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "json", value = "json", required = true, dataType = "string", defaultValue = "{\"collectionId\":\"\"}", paramType = "form")})
	public Response create(@Context HttpServletRequest req, @FormDataParam("file") InputStream file,
			@ApiParam(value = "File details", required = false, hidden = true) @FormDataParam("file") FormDataContentDisposition fileDetail,
			@ApiParam(value = "json", hidden = true) @FormDataParam(value = "json") String json) {
		final String origName = fileDetail != null ? fileDetail.getFileName() : null;
		return RestProcessUtils.buildJSONResponse(createItem(req, file, json, origName));
	}

	@PUT
	@Path("/{id}")
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@ApiOperation(value = "Update an item", response = DefaultItemTO.class, notes = "Update an item with (optional) a file. File can be defined either as (by order of priority):"
			+ "<div> 1) form parameter (multipart/form-data)</div>"
			+ "<div> 2) json parameter: \"fetchUrl\" : \"http://example.org/myFile.png\" (myFile.png will be uploaded in imeji) </div>"
			+ "<div> 3) json parameter \"referenceUrl\" : \"http://example.org/myFile.png\" (myFile.png will be only referenced in imeji, i.e. not uploaded)</div>")
	@Produces(MediaType.APPLICATION_JSON)
	@ApiImplicitParams({
			@ApiImplicitParam(name = "json", value = "json", required = true, dataType = "string", defaultValue = "{\"collectionId\":\"\"}", paramType = "form")})
	public Response update(@Context HttpServletRequest req, @FormDataParam("file") InputStream file,
			@ApiParam(required = true, hidden = true) @FormDataParam("json") String json,
			@ApiParam(value = "File details", required = false, hidden = true) @FormDataParam("file") FormDataContentDisposition fileDetail,
			@ApiParam(required = true, value = "Item id") @PathParam("id") String id) {
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
	@ApiOperation(value = "Delete an item by id")
	@Produces(MediaType.APPLICATION_JSON)
	public Response delete(@Context HttpServletRequest req, @PathParam("id") String id) {
		return RestProcessUtils.buildJSONResponse(deleteItem(req, id));
	}

}
