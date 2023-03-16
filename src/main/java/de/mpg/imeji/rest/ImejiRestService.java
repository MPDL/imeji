package de.mpg.imeji.rest;

import de.mpg.imeji.rest.resources.ImejiResource;
import io.swagger.v3.jaxrs2.integration.JaxrsOpenApiContextBuilder;
import io.swagger.v3.jaxrs2.integration.resources.OpenApiResource;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.integration.OpenApiConfigurationException;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.servlet.ServletConfig;
import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Context;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationPath("/rest")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "BasicAuth", type = SecuritySchemeType.HTTP, scheme = "basic")
@io.swagger.v3.oas.annotations.security.SecurityScheme(name = "ApiKeyAuth", type = SecuritySchemeType.APIKEY, scheme = "bearer")
public class ImejiRestService extends ResourceConfig {
  Logger LOGGER = LogManager.getLogger(ImejiRestService.class);

  public static final int CURRENT_VERSION = 2;


  public ImejiRestService(@Context ServletConfig servletConfig) {
    super(MultiPartFeature.class);
    packages(ImejiResource.class.getPackage().getName());
    // register(MultiPartFeature.class);


    OpenAPI oas = new OpenAPI();
    Info info = new Info().title("imeji API").version("v" + CURRENT_VERSION);
    oas.info(info);

    //SecurityScheme sc = new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic");
    //oas.getComponents().addSecuritySchemes("BasicAuth", sc);
    SecurityRequirement sr = new SecurityRequirement().addList("BasicAuth").addList("ApiKeyAuth");
    oas.security(Stream.of(sr).collect(Collectors.toList()));

    Server server = new Server().url("/imeji");
    oas.servers(Stream.of(server).collect(Collectors.toList()));
    SwaggerConfiguration oasConfig = new SwaggerConfiguration()
            .openAPI(oas)
            .prettyPrint(true)
            .resourcePackages(Stream.of(ImejiRestService.class.getPackage().getName()).collect(Collectors.toSet()))
            .alwaysResolveAppPath(true);

    register(new OpenApiResource().openApiConfiguration(oasConfig));
    /*
    try {
      new JaxrsOpenApiContextBuilder().servletConfig(servletConfig).application(this).openApiConfiguration(oasConfig).buildContext(true);
    } catch (OpenApiConfigurationException e) {
      throw new RuntimeException(e.getMessage(), e);
    }

     */
    //register(io.swagger.jaxrs.listing.ApiListingResource.class);
    //register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    /*
    final BeanConfig beanConfig = new BeanConfig();
    beanConfig.setTitle("imeji API");
    beanConfig.setVersion("v" + CURRENT_VERSION);
    beanConfig.setBasePath("/imeji/rest");
    beanConfig.setResourcePackage(ImejiResource.class.getPackage().getName());
    beanConfig.setScan(true);
    
     */
  }

}
