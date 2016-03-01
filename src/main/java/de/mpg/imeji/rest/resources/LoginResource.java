/*package de.mpg.imeji.rest.resources;

import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;

import de.mpg.imeji.rest.process.AdminProcess;
import de.mpg.imeji.rest.process.RestProcessUtils;

*//**
 * API Method to login
 * 
 * @author bastiens
 *
 *//*
@Path("/login")
@Api(value = "rest/login", description = "Login to the API")
public class LoginResource {
  @POST
  @ApiOperation(value = "Login to API",
      notes = "You can either login with: " + "<br/>"
          + "- Basic Authentication, ex: Authorization: Basic &ltbase64 encoded email:password&gt; <br/>"
          + "- API Key, ex: Authorization Bearer api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@HeaderParam("authorization") String authHeader) {
    return RestProcessUtils.buildJSONResponse(AdminProcess.login(authHeader));
  }
}
*/