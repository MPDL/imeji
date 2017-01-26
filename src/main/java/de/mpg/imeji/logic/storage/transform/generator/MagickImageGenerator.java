package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;

/**
 * {@link ImageGenerator} implemented with imagemagick
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MagickImageGenerator implements ImageGenerator {
  private boolean enabled = false;
  private static final Logger LOGGER = Logger.getLogger(MagickImageGenerator.class);

  /**
   * Default constructor
   */
  public MagickImageGenerator() {
    try {
      enabled = Boolean.parseBoolean(PropertyReader.getProperty("imeji.imagemagick.enable"));
    } catch (final Exception e) {
      throw new RuntimeException("Error reading property imeji.imagemagick.enable", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generateJPG(File file, String extension) {
    if (enabled) {
      try {
        return ImageMagickUtils.convertToJPEG(file, extension);
      } catch (final Exception e) {
        LOGGER.warn("Error with imagemagick: ", e);
        return null;
      }
    }
    return null;
  }
}
