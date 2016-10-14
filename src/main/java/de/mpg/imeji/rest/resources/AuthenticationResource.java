package de.mpg.imeji.rest.resources;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import de.mpg.imeji.rest.process.AdminProcess;
import de.mpg.imeji.rest.process.RestProcessUtils;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;


/**
 * API Method to login
 *
 * @author bastiens
 *
 */

@Path("/")
@Api(value = "rest/authentication", description = "Authentication")
public class AuthenticationResource {

  @POST
  @Path("/login")
  @ApiOperation(value = "Login to API",
      notes = "You can either login with: " + "<br/>"
          + "- Basic Authentication, ex: Authorization: Basic &ltbase64 encoded email:password&gt; <br/>"
          + "- API Key, ex: Authorization Bearer api_key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response login(@HeaderParam("Authorization") String authHeader) {
    return RestProcessUtils.buildJSONResponse(AdminProcess.login(authHeader));
  }

  @POST
  @Path("/logout")
  @ApiOperation(value = "Logout from the API",
      notes = "The APIKey of the current User (according to authorization header) will be invalidated. User will need to login with Basic Authentication to get a new Key")
  @Produces(MediaType.APPLICATION_JSON)
  public Response logout(@HeaderParam("authorization") String authHeader) {
    return RestProcessUtils.buildJSONResponse(AdminProcess.logout(authHeader));
  }

  @GET
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/login")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodGetlogin() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidResource());
  }

  @GET
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/logout")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodGetlogout() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidResource());
  }

  @PUT
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/login")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodPutlogin() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  @PUT
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/logout")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodPutlogout() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  @DELETE
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/login")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodDeletelogin() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  @DELETE
  // @Path("/{text: (/!((items|collections|albums|profiles|storage)/?)).*}")
  @Path("/logout")
  // Pattern.compile
  // ("(((https?|ftp|gopher|telnet|file|Unsure|http):)*((/)|(//)|(\\\\))+[\\w\\d:#@%/;$()~_?\\+-=\\\\\\.&]*)");
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodDeletelogout() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  /*
   * NOTE
   * 
   * DO NOT DELETE FOLLOWING METHODS HERE They match the URI Path for all other methods which are
   * not supported. Thus we get proper error message and no entity returned (instead of previously
   * returned jsf page in the response which is stated in the web.xml) If method is not already
   * matched by any of the other resources, these error methods will be given
   * 
   * FOR some reason, for /login and /logout there have to be special methods (below matcher does
   * not catch them).
   * 
   */

  @POST

  @Path("/{text: (/?.*)}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodPost() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  @PUT
  @Path("/{text: (/?.*)}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodPut() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }

  @GET
  @Path("/{text: (/?.*)}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodGet() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidResource());
  }

  @DELETE
  @Path("/{text: (/?.*)}")
  @Produces(MediaType.APPLICATION_JSON)
  public Response badMethodDelete() {
    return RestProcessUtils.buildJSONResponse(AdminProcess.invalidMethod());
  }


}
