package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.util.PdfUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * {@link ImageGenerator} to generate image out of pdf
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PdfImageGenerator implements ImageGenerator {
  private static final Logger LOGGER = Logger.getLogger(PdfImageGenerator.class);

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generateJPG(File file, String extension) {
    if (StorageUtils.getMimeType(extension).equals("application/pdf")) {
      try {
        return PdfUtils.pdfToImage(file);
      } catch (final IOException e) {
        LOGGER.error("Error reading pdf file", e);
      }
    }
    return null;
  }
}
