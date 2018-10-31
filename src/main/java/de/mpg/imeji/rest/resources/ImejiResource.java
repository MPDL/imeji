package de.mpg.imeji.rest.resources;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

public interface ImejiResource {

	public Response readAll(HttpServletRequest req, String q, int offset, int size);

	public Response read(HttpServletRequest req, String id);

	public Response create(HttpServletRequest req);

	public Response delete(HttpServletRequest req, String id);

	/**
	 * The Default size of list when searching/retrieving data from the API
	 */
	public static final String DEFAULT_LIST_SIZE = "20";

}
