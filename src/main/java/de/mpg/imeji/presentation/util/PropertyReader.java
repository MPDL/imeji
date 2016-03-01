package de.mpg.imeji.presentation.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Utility class to read property files in Tomcat and JBoss application
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PropertyReader {
  private static Properties properties;
  private static final String DEFAULT_PROPERTY_FILE = "imeji.properties";
  private static URL solution;
  private static String version = null;
  private static final Logger LOGGER = Logger.getLogger(PropertyReader.class);

  /**
   * private constructor
   */
  private PropertyReader() {}

  /**
   * Gets the value of a property for the given key from the system properties or the escidoc
   * property file. It is always tried to get the requested property value from the system
   * properties. This option gives the opportunity to set a specific property temporary using the
   * system properties. If the requested property could not be obtained from the system properties
   * the escidoc property file is accessed. (For details on access to the properties file see class
   * description.)
   * 
   * @param key The key of the property.
   * @return The value of the property.
   * @throws IOException
   * @throws URISyntaxException
   */
  public static String getProperty(String key) throws IOException, URISyntaxException {
    // First check system properties
    String value = System.getProperty(key);
    if (value != null) {
      return value;
    }
    // Check properties file
    if (properties == null) {
      loadProperties();
    }
    // Get the property
    value = properties.getProperty(key);
    // Logger.getLogger(PropertyReader.class).info("framework URL: "+value);
    return value;
  }

  public static Properties getProperties() throws IOException, URISyntaxException {
    if (properties == null) {
      loadProperties();
    }
    return properties;
  }

  /**
   * Load the properties from the location defined by the system property
   * <code>pubman.properties.file</code>. If this property is not set the default file path
   * <code>pubman.properties</code> is used.
   * 
   * @throws IOException If the properties file could not be found neither in the file system nor in
   *         the classpath.
   * @throws URISyntaxException
   */
  public static void loadProperties() throws IOException, URISyntaxException {
    String propertiesFile = null;
    Properties solProperties = new Properties();
    try {
      solution = PropertyReader.class.getClassLoader().getResource("solution.properties");
    } catch (Exception e) {
      Logger.getLogger(PropertyReader.class)
          .warn("WARNING: solution.properties not found: " + e.getMessage());
    }
    if (solution != null) {
      Logger.getLogger(PropertyReader.class).info("Solution URI is " + solution.toString());
      InputStream in = getInputStream("solution.properties");
      solProperties.load(in);
      String appname = solProperties.getProperty("appname");
      version = solProperties.getProperty("imeji.application.version");
      propertiesFile = appname + ".properties";
    } else {
      // Use Default location of properties file
      propertiesFile = DEFAULT_PROPERTY_FILE;
      Logger.getLogger(PropertyReader.class)
          .debug("solution.properties file not found. Trying default.");
    }
    InputStream instream = getInputStream(propertiesFile);
    properties = new Properties();
    properties.load(instream);
    cleanUp(properties).putAll(solProperties);
  }


  /**
   * Retrieves the Inputstream of the given file path. First the resource is searched in the file
   * system, if this fails it is searched using the classpath.
   * 
   * @param filepath The path of the file to open.
   * @return The inputstream of the given file path.
   * @throws IOException If the file could not be found neither in the file system nor in the
   *         classpath.
   */
  public static InputStream getInputStream(String filepath) throws IOException {
    InputStream instream = null;
    // First try to search in file system
    try {
      String serverConfDirectory;
      if (System.getProperty("jboss.server.config.dir") != null) {
        serverConfDirectory = System.getProperty("jboss.server.config.dir");
      } else if (System.getProperty("catalina.base") != null) {
        serverConfDirectory = System.getProperty("catalina.base") + "/conf";
      } else if (System.getProperty("catalina.home") != null) {
        serverConfDirectory = System.getProperty("catalina.home") + "/conf";
      } else {
        serverConfDirectory = "/src/test/resources";
      }
      LOGGER.info("loading properties from " + serverConfDirectory + "/" + filepath);
      instream = new FileInputStream(serverConfDirectory + "/" + filepath);
    } catch (Exception e) {
      // try to get resource from classpath
      URL url = PropertyReader.class.getClassLoader().getResource(filepath);
      if (url != null) {
        instream = url.openStream();
      }
    }
    if (instream == null) {
      throw new FileNotFoundException(filepath);
    }
    return instream;
  }

  public static String getVersion() {
    return version;
  }

  /**
   * Clean up properties
   * 
   * @param props
   * @return
   */

  private static Properties cleanUp(Properties props) {
    // Trim values
    for (Entry<Object, Object> e : props.entrySet()) {
      e.setValue(((String) e.getValue()).trim());
    }
    return props;
  }

}
