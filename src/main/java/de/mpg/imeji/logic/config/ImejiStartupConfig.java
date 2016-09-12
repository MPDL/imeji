package de.mpg.imeji.logic.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Properties;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Startup configuration. Read only. Must be edited by sysadmin on server
 * 
 * @author saquet
 *
 */
public class ImejiStartupConfig {
  private enum ENTRIES {
    REINDEX;
  }

  private static Logger LOGGER = Logger.getLogger(ImejiStartupConfig.class);
  private static Properties config;
  private static File configFile;

  public ImejiStartupConfig() {
    try {
      readConfigurationFile();
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Error reading startup conf file: " + configFile.getAbsolutePath(), e);
    }
  }

  /**
   * Get the Configuration File from the filesystem. If not existing, create a new one with default
   * values
   *
   * @throws IOException
   * 
   * @throws URISyntaxException
   */
  private synchronized void readConfigurationFile() throws IOException, URISyntaxException {
    config = new Properties();
    configFile = new File(PropertyReader.getProperty("imeji.tdb.path") + "/startup.properties");
    if (!configFile.exists()) {
      configFile.createNewFile();
      setDefaultConfig();
    }
    FileInputStream in = new FileInputStream(configFile);
    config.load(in);
  }

  /**
   * Write the Default Configuration to the Disk. This should be called when the Configuration is
   * initialized for the first time.
   */
  private synchronized void setDefaultConfig() {
    config = new Properties();
    config.setProperty(ENTRIES.REINDEX.name(), "false");
    saveConfig();
  }

  /**
   * Save the configuration in the config file
   */
  public void saveConfig() {
    try {
      config.store(new FileOutputStream(configFile), "imeji startup configuration File");
      LOGGER.info("saving imeji startup config");
    } catch (Exception e) {
      LOGGER.error("Error saving startup configuration:", e);
    }
  }

  /**
   * True if a reindex must be done by startup
   * 
   * @return
   */
  public boolean doReIndex() {
    return Boolean.parseBoolean(config.getProperty(ENTRIES.REINDEX.name()));
  }

}
