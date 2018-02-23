package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Generate a simple icon for an audio file
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleAudioImageGenerator implements ImageGenerator {
  private static final Logger LOGGER = Logger.getLogger(SimpleAudioImageGenerator.class);
  private static final String PATH_TO_AUDIO_ICON = "images/audio_file_icon.jpg";

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generateJPG(File file, String extension) {
    if (StorageUtils.getMimeType(extension).contains("audio")) {
      try {
        File copy = TempFileUtil.createTempFile("audio", extension);
        FileUtils.copyFile(new File(SimpleAudioImageGenerator.class.getClassLoader()
            .getResource(PATH_TO_AUDIO_ICON).toURI()), copy);
        return copy;
      } catch (final URISyntaxException | IOException e) {
        LOGGER.error("Error creating thunmbnail", e);
      }
    }
    return null;
  }
}
