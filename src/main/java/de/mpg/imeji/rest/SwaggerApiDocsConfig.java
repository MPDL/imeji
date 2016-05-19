package de.mpg.imeji.rest;

import javax.servlet.ServletConfig;

/**
 * Created by vlad on 10.07.15.
 */

import com.wordnik.swagger.config.ConfigFactory;
import com.wordnik.swagger.jersey.config.JerseyJaxrsConfig;

public class SwaggerApiDocsConfig extends JerseyJaxrsConfig {
  private static final long serialVersionUID = -5843988574445185846L;

  @Override
  public void init(ServletConfig servletConfig) {
    super.init(servletConfig);
    ConfigFactory.config().setBasePath(
        servletConfig.getServletContext().getContextPath() + ConfigFactory.config().getBasePath());
  }
}
