package de.mpg.imeji.logic.config;

import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Configuration done from the server (in imeji.properties)
 *
 * @author bastiens
 *
 */
public class ImejiProperties {
  /**
   * True if Digilib is enabled
   */
  private boolean digilibEnabled = false;
  /**
   * The base of the path to the internal storage
   */
  private String internalStorageBase = "files";
  /**
   * The base of the uri of imeji objects
   */
  private static String baseURI;
  private static String applicationURL;

  private static final Logger LOGGER = Logger.getLogger(ImejiProperties.class);

  /**
   * Construct and load the properties from the server
   */
  public ImejiProperties() {
    load();
  }

  /**
   * Load the properties from the server
   */
  public void load() {
    try {
      PropertyReader.loadProperties();
      digilibEnabled = Boolean.parseBoolean(getProperty("imeji.digilib.enable"));
      internalStorageBase = FilenameUtils
          .getBaseName(FilenameUtils.normalizeNoEndSeparator(getProperty("imeji.storage.path")));
      applicationURL = StringHelper.normalizeURI(getProperty("imeji.instance.url"));
      readBaseUri();
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Error reading imeji.properties", e);
    }
  }


  /**
   * Function reads each property from imeji.properties file
   *
   * @param key - property name
   * @return String - trimmed value of key
   */
  public String getProperty(String key) {
    try {
      return PropertyReader.getProperty(key).trim();
    } catch (Exception e) {
      throw new RuntimeException("Error reading properties: ", e);
    }
  }

  /**
   * Read in the property the base Uri
   */
  private void readBaseUri() {
    try {
      baseURI =
          StringHelper.normalizeURI(PropertyReader.getProperty("imeji.jena.resource.base_uri"));
    } catch (Exception e) {
      throw new RuntimeException("Error reading properties: ", e);
    }
    if (baseURI == null || baseURI.trim().equals("/")) {
      baseURI = applicationURL;
    }
    if (baseURI == null) {
      throw new RuntimeException("Error in properties. Check property: imeji.instance.url");
    }
    ObjectHelper.baseUri = baseURI;
  }

  /**
   * @return the digilibEnabled
   */
  public boolean isDigilibEnabled() {
    return digilibEnabled;
  }

  /**
   * @return the internalStorageBase
   * @throws URISyntaxException
   * @throws IOException
   */
  public String getInternalStorageBase() {
    return internalStorageBase;
  }

  /**
   * @return the baseURI
   */
  public String getBaseURI() {
    return baseURI;
  }

  /**
   * Static getter
   *
   * @return
   */
  public static String baseURI() {
    return baseURI;
  }

  /**
   * @return the applicationURL
   */
  public String getApplicationURL() {
    return applicationURL;
  }

  public String getApplicationURI() {
    return applicationURL.substring(0, applicationURL.length() - 1);
  }
}
