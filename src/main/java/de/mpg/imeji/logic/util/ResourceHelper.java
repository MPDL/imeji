package de.mpg.imeji.logic.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by vlad on 11.02.15.
 */
public class ResourceHelper {

  /**
   * Private Constructor
   */
  private ResourceHelper() {
    // avoid constructor
  }

  /**
   * Read a file defined by its path, and return the content as String
   *
   * @param path
   * @return
   * @throws IOException
   */
  public static String getStringFromPath(String path) throws IOException {
    return new String(Files.readAllBytes(Paths.get(path)), "UTF-8");
  }

}
