package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;

import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * {@link ImageGenerator} for basics format. Doesn't need any dependency
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleImageGenerator extends ImageGenerator {
  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generate(byte[],
   * java.lang.String, int, int)
   */
  @Override
  public File generatePreview(File file, String extension) {

    return ImageUtils.toJpeg(file, StorageUtils.getMimeType(extension));
  }

  @Override
  protected boolean generatorSupportsMimeType(String fileExtension) {
    boolean isImage = StorageUtils.getMimeType(fileExtension).contains("image");
    return isImage;
  }
}
