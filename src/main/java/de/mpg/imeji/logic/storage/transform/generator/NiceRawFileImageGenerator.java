package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.TempFileUtil;

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
      URL urlResource = RawFileImageGenerator.class.getClassLoader()
          .getResource(PATH_TO_ICONS + extension + ".jpg");
      if (urlResource != null) {
        File res = TempFileUtil.createTempFile("NiceRawFile",
            FilenameUtils.getExtension(new File(urlResource.toURI()).getName()));
        FileUtils.copyFile(new File(urlResource.toURI()), res);
        if (res.exists() && res.length() > 0) {
          return res;
        }
      }
      return null;
    } catch (URISyntaxException | IOException e) {
      LOGGER.error("Error creating icon for " + extension, e);
      return null;
    }
  }

}
