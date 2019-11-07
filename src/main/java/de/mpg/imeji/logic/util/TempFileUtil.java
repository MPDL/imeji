package de.mpg.imeji.logic.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Utility to work with temp files
 *
 * @author bastiens
 *
 */
public class TempFileUtil {

  private static final Logger LOGGER = LogManager.getLogger(TempFileUtil.class);
  public static final String IMEJI_TEMP_FILE_PREFIX = "imeji";
  private static File tempDir;

  /**
   * Private Constructor
   * 
   * @throws URISyntaxException
   * @throws IOException
   */
  private TempFileUtil() {

  }

  public static File getTempDirectory() {
    return tempDir;
  }

  /**
   * Gets the temp directory. Creates a new temp directory if none exists.
   * 
   * @return The temp directory
   */
  public static File getOrCreateTempDirectory() {
    try {
      File newTempDir = new File(
          PropertyReader.getProperty("imeji.storage.path") + StringHelper.fileSeparator + ".." + StringHelper.fileSeparator + "tmp");
      if (!newTempDir.exists()) {
        newTempDir.mkdirs();
        tempDir = newTempDir;
      }
      return tempDir;
    } catch (IOException e) {
      LOGGER.error("Error creating the temp directory.", e);
      return null;
    }
  }

  /**
   * Create a temp {@link File}. This method should be used to ensure that all temp files are
   * correctly removed
   *
   * @param name
   * @param extension
   * @return
   * @throws IOException
   */
  public static File createTempFile(String name, String extension) throws IOException {
    File tempDir = getOrCreateTempDirectory();
    return File.createTempFile(IMEJI_TEMP_FILE_PREFIX + name, extension, tempDir);
  }

}
