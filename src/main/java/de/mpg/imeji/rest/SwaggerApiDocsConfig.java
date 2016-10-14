package de.mpg.imeji.rest;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import io.swagger.jaxrs.config.BeanConfig;
import io.swagger.jersey.config.JerseyJaxrsConfig;
import io.swagger.models.Info;

/**
 * Created by vlad on 10.07.15.
 */



public class SwaggerApiDocsConfig extends JerseyJaxrsConfig {
  private static final long serialVersionUID = -5843988574445185846L;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    Info info = new Info().title("imeji API");
    BeanConfig beanConfig = new BeanConfig();
    beanConfig.setBasePath(config.getServletContext().getContextPath());
    beanConfig.setInfo(info);
  }

  // @Override
  // public void init(ServletConfig servletConfig) {
  // super.init(servletConfig);
  // ConfigFactory.config().setBasePath(
  // servletConfig.getServletContext().getContextPath() + ConfigFactory.config().getBasePath());
  // }
}
