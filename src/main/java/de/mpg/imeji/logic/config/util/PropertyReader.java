package de.mpg.imeji.logic.config.util;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Utility class to read property files in Tomcat and JBoss applications.
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PropertyReader {

  private static Properties properties;
  private static final String IMEJI_PROPERTY_FILE = "imeji.properties";
  private static final String VERSION_PROPERTY_FILE = "version.properties";
  private static final Logger LOGGER = LogManager.getLogger(PropertyReader.class);

  /**
   * private constructor
   */
  private PropertyReader() {
    // Avoid construction
  }

  /**
   * Gets the value of a property for the given key from the system properties or the imeji
   * properties file.
   * <p>
   * It is always tried to get the requested property value from the system properties. This option
   * gives the opportunity to set a specific property temporary using the system properties. If the
   * requested property could not be obtained from the system properties the imeji properties file
   * is accessed. (For details on access to the imeji properties file see
   * <code>loadProperties()</code> method.)
   *
   * @param key The key of the property.
   * @return The value of the property.
   * @throws IOException
   */
  public static String getProperty(String key) throws IOException {
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
    return value;
  }

  /**
   * Load all properties from the imeji properties files. <br>
   * (For details on access to the imeji.properties file see <code>loadImejiProperties()</code>
   * method.)
   *
   * @throws IOException If the properties files could not be found or read.
   */
  public static void loadProperties() throws IOException {
    properties = new Properties();
    Properties imejiProperties = loadImejiProperties();
    Properties versionProperties = loadVersionProperties();

    properties.putAll(imejiProperties);
    properties.putAll(versionProperties);
    cleanUp(properties);
  }

  private static Properties loadVersionProperties() throws IOException {
    InputStream inputStream = PropertyReader.class.getClassLoader().getResourceAsStream(VERSION_PROPERTY_FILE);
    if (inputStream == null) {
      throw new FileNotFoundException(VERSION_PROPERTY_FILE);
    }

    Properties versionProperties = new Properties();
    versionProperties.load(inputStream);
    return versionProperties;
  }

  /**
   * Load the imeji.properties from the server configuration (conf) directory, if a server is
   * detected. Otherwise the imeji.properties file is loaded from the classpath.
   * <p>
   * If a server is detected but the properties file could not be found in the server conf directory
   * or if no server is detected and the properties could not be found in the classpath a
   * <code>FileNotFoundException</code> is thrown.
   *
   * @return the imeji properties
   * @throws FileNotFoundException If a server is detected but the properties file could not be
   *         found in the server conf directory or if no server is detected and the properties could
   *         not be found in the classpath.
   * @throws IOException If the properties file could not be read.
   */
  private static Properties loadImejiProperties() throws IOException {
    String serverConfDirectory = null;
    if (System.getProperty("jboss.server.config.dir") != null) {
      serverConfDirectory = System.getProperty("jboss.server.config.dir");
    } else if (System.getProperty("catalina.base") != null) {
      serverConfDirectory = System.getProperty("catalina.base") + "/conf";
    } else if (System.getProperty("catalina.home") != null) {
      serverConfDirectory = System.getProperty("catalina.home") + "/conf";
    }

    InputStream inputStream = null;
    if (serverConfDirectory != null) {
      String propertiesFilePath = serverConfDirectory + "/" + IMEJI_PROPERTY_FILE;
      LOGGER.info("Loading properties from " + propertiesFilePath);
      //Throws a FileNotFoundException if the properties file is not found in the server-conf directory.
      inputStream = new FileInputStream(propertiesFilePath);
    } else {
      //Load imeji.properties from classpath. Used in the context of testing, in which no server is running.
      //Warning: If no properties file exists in the test directory the default imeji.properties is loaded,
      //which may cause the deletion of the data of the local imeji instance by the tests.
      LOGGER.warn("No server directory found. Loading " + IMEJI_PROPERTY_FILE + " from classpath."
          + " In case of testing this will load the properties file from the test directory."
          + " If no properties file exists in the test directory the default " + IMEJI_PROPERTY_FILE + " is loaded.");
      inputStream = PropertyReader.class.getClassLoader().getResourceAsStream(IMEJI_PROPERTY_FILE);
      if (inputStream == null) {
        throw new FileNotFoundException(IMEJI_PROPERTY_FILE);
      }
    }

    Properties imejiProperties = new Properties();
    imejiProperties.load(inputStream);
    return imejiProperties;
  }

  private static Properties cleanUp(Properties props) {
    // Trim values
    for (final Entry<Object, Object> e : props.entrySet()) {
      e.setValue(((String) e.getValue()).trim());
    }
    return props;
  }

}
