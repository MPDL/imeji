package de.mpg.imeji.rest;

import javax.ws.rs.ApplicationPath;

import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import de.mpg.imeji.rest.resources.ImejiResource;
import io.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/rest/*")
public class ImejiRestService extends ResourceConfig {
  Logger LOGGER = Logger.getLogger(ImejiRestService.class);

  public static final int CURRENT_VERSION = 2;

  public ImejiRestService() {
    super(MultiPartFeature.class);
    packages(ImejiResource.class.getPackage().getName());
    // register(MultiPartFeature.class);
    register(io.swagger.jaxrs.listing.ApiListingResource.class);
    register(io.swagger.jaxrs.listing.SwaggerSerializers.class);
    final BeanConfig beanConfig = new BeanConfig();
    beanConfig.setTitle("imeji API");
    beanConfig.setVersion("v" + CURRENT_VERSION);
    beanConfig.setBasePath("/imeji/rest");
    beanConfig.setResourcePackage(ImejiResource.class.getPackage().getName());
    beanConfig.setScan(true);
  }

}
