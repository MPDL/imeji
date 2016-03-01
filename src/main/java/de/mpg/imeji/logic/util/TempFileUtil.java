package de.mpg.imeji.logic.util;

import java.io.File;
import java.io.IOException;

import de.mpg.imeji.logic.jobs.CleanTempFilesJob;

/**
 * Utility to work with temp files
 * 
 * @author bastiens
 * 
 */
public class TempFileUtil {

  public static final String IMEJI_TEMP_FILE_PREFIX = "imeji";

  /**
   * Private Constructor
   */
  private TempFileUtil() {
    // private constructor
  }

  /**
   * Create a temp {@link File}. This method should be used to ensure that all temp files are
   * correctly removed by the {@link CleanTempFilesJob}
   * 
   * @param name
   * @param extension
   * @return
   * @throws IOException
   */
  public static File createTempFile(String name, String extension) throws IOException {
    return File.createTempFile(IMEJI_TEMP_FILE_PREFIX + name, extension);
  }

}
