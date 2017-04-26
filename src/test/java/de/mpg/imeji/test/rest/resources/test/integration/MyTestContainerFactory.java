package de.mpg.imeji.test.rest.resources.test.integration;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

import javax.servlet.Servlet;
import javax.ws.rs.ProcessingException;

import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.grizzly.servlet.ServletRegistration;
import org.glassfish.grizzly.servlet.WebappContext;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.spi.TestContainer;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.uri.UriComponent;

import com.google.common.collect.ImmutableMap;

/**
 * The class overrides TestContainerFactory with GrizzlyWebContainerFactory. The GrizzlyWebContainer
 * fully supports HttpServlet, that is needed to test @Context injection variables in tests
 *
 * @author vmakarenko
 *
 */
public class MyTestContainerFactory implements TestContainerFactory {

  public static final String REST_CONTEXT_PATH = "/rest";
  public static final String STATIC_CONTEXT_PATH = "/static";
  public static final String STATIC_CONTEXT_STORAGE = "src/test/resources/storage";
  public static final String STATIC_CONTEXT_REST = "src/test/resources/rest";

  @Override
  public TestContainer create(final URI baseUri, final DeploymentContext deploymentContext)
      throws IllegalArgumentException {
    return new TestContainer() {
      private HttpServer server;

      @Override
      public ClientConfig getClientConfig() {
        return null;
      }

      @Override
      public URI getBaseUri() {
        return baseUri;
      }

      @Override
      public void start() {
        try {
          this.server = create(baseUri, ServletContainer.class, null,
              ImmutableMap.of("jersey.config.server.provider.packages",
                  "de.mpg.imeji.rest.resources", "jersey.config.server.provider.classnames",
                  "org.glassfish.jersey.filter.LoggingFilter;org.glassfish.jersey.media.multipart.MultiPartFeature"),
              null);

        } catch (ProcessingException | IOException e) {
          throw new TestContainerException(e);
        }
      }

      @Override
      public void stop() {
        this.server.shutdownNow();
      }
    };

  }

  private static HttpServer create(URI u, Class<? extends Servlet> c, Servlet servlet,
      Map<String, String> initParams, Map<String, String> contextInitParams) throws IOException {
    if (u == null) {
      throw new IllegalArgumentException("The URI must not be null");
    }

    String path = u.getPath();
    if (path == null) {
      throw new IllegalArgumentException("The URI path, of the URI " + u + ", must be non-null");
    } else if (path.isEmpty()) {
      throw new IllegalArgumentException("The URI path, of the URI " + u + ", must be present");
    } else if (path.charAt(0) != '/') {
      throw new IllegalArgumentException(
          "The URI path, of the URI " + u + ". must start with a '/'");
    }

    path = String.format("/%s", UriComponent.decodePath(u.getPath(), true).get(1).toString());

    WebappContext context = new WebappContext("GrizzlyContext", REST_CONTEXT_PATH);
    ServletRegistration registration;
    if (c != null) {
      registration = context.addServlet(c.getName(), c);
    } else {
      registration = context.addServlet(servlet.getClass().getName(), servlet);
    }
    registration.addMapping("/*");

    if (contextInitParams != null) {
      for (Map.Entry<String, String> e : contextInitParams.entrySet()) {
        context.setInitParameter(e.getKey(), e.getValue());
      }
    }

    if (initParams != null) {
      registration.setInitParameters(initParams);
    }

    HttpServer server = GrizzlyHttpServerFactory.createHttpServer(u);

    context.deploy(server);

    server.getServerConfiguration().addHttpHandler(
        new StaticHttpHandler(STATIC_CONTEXT_REST, STATIC_CONTEXT_STORAGE), STATIC_CONTEXT_PATH);

    return server;
  }

}
