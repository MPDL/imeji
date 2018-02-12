package de.mpg.imeji.logic.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Utility to work with temp files
 *
 * @author bastiens
 *
 */
public class TempFileUtil {

  public static final String IMEJI_TEMP_FILE_PREFIX = "imeji";
  private static final File TEMP_DIR = initTempDirectory();

  /**
   * Private Constructor
   * 
   * @throws URISyntaxException
   * @throws IOException
   */
  private TempFileUtil() {

  }

  private static File initTempDirectory() {
    try {
      File f = new File(PropertyReader.getProperty("imeji.storage.path")
          + StringHelper.fileSeparator + ".." + StringHelper.fileSeparator + "tmp");
      if (!f.exists()) {
        f.mkdirs();
      }
      return f;
    } catch (IOException | URISyntaxException e) {
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
    return File.createTempFile(IMEJI_TEMP_FILE_PREFIX + name, extension, TEMP_DIR);
  }

}
