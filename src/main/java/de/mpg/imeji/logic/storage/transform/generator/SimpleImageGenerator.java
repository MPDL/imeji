package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;

import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * {@link ImageGenerator} for basics format. Doesn't need any dependency
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleImageGenerator implements ImageGenerator {
  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generate(byte[], java.lang.String,
   * int, int)
   */
  @Override
  public File generateJPG(File file, String extension) {
    if (StorageUtils.getMimeType(extension).contains("image")) {
      return ImageUtils.toJpeg(file, StorageUtils.getMimeType(extension));
    }
    return null;
  }
}
