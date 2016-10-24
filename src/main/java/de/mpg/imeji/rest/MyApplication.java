package de.mpg.imeji.rest;

import org.apache.log4j.Logger;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;

import de.mpg.imeji.rest.resources.ImejiResource;
import io.swagger.jaxrs.config.BeanConfig;

public class MyApplication extends ResourceConfig {
  Logger LOGGER = Logger.getLogger(MyApplication.class);

  public static final int CURRENT_VERSION = 1;

  public MyApplication() {
    packages(ImejiResource.class.getPackage().getName());
    if (LOGGER.isDebugEnabled()) {
      register(LoggingFilter.class);
    }
    register(MultiPartFeature.class);
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setTitle("imeji API");
    beanConfig.setVersion("v" + CURRENT_VERSION);
    beanConfig.setBasePath("/imeji/rest");
    beanConfig.setResourcePackage(ImejiResource.class.getPackage().getName());
    beanConfig.setScan(true);
  }
}
