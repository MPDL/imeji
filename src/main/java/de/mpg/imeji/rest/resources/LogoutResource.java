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
*/
/**
 * API Mehod to logout
 * 
 * @author bastiens
 *
 */
/*
@Path("/logout")
@Api(value = "rest/logout", description = "Logout")
public class LogoutResource {

  @POST
  @ApiOperation(value = "Logout from the API",
      notes = "The APIKey of the current User (according to authorization header) will be invalidated. User will need to login with Basic Authentication to get a new Key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@HeaderParam("authorization") String authHeader) {
    return RestProcessUtils.buildJSONResponse(AdminProcess.logout(authHeader));
  }

}
*/
