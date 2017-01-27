package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.net.URISyntaxException;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * {@link ImageGenerator} that uses a fixed jpg-file for each extension
 * 
 * @author jandura
 *
 */

public class NiceRawFileImageGenerator implements ImageGenerator {

  private static final Logger LOGGER = Logger.getLogger(RawFileImageGenerator.class);

  public static final String PATH_TO_ICONS = "images/icons/";

  @Override
  public File generateJPG(File file, String extension) throws ImejiException {
    try {
      File res = new File(RawFileImageGenerator.class.getClassLoader()
          .getResource(PATH_TO_ICONS + extension + ".jpg").toURI());
      if (res.exists()) {
        return res;
      }
      return null;
    } catch (URISyntaxException e) {
      LOGGER.error("Error creating icon", e);
      return null;
    }
  }

}
