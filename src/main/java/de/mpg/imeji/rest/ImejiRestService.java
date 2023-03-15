package de.mpg.imeji.rest;

import de.mpg.imeji.rest.resources.ImejiResource;
import io.swagger.v3.oas.integration.SwaggerConfiguration;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@ApplicationPath("/rest/*")
public class ImejiRestService extends ResourceConfig {
  Logger LOGGER = LogManager.getLogger(ImejiRestService.class);

  public static final int CURRENT_VERSION = 2;

  public ImejiRestService() {
    super(MultiPartFeature.class);
    packages(ImejiResource.class.getPackage().getName());
    // register(MultiPartFeature.class);


    OpenAPI oas = new OpenAPI();
    Info info = new Info().title("imeji API").version("v" + CURRENT_VERSION);

    oas.info(info);
    SwaggerConfiguration oasConfig = new SwaggerConfiguration().openAPI(oas).prettyPrint(true)
        .resourcePackages(Stream.of(ImejiResource.class.getPackage().getName()).collect(Collectors.toSet()));

    register(oasConfig);
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
