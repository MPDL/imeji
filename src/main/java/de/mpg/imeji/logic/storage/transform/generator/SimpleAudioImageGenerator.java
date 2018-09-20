package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.storage.transform.generator.icons.ImejiFileIcon;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Generate a simple icon for an audio file
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SimpleAudioImageGenerator extends ImageGenerator {
  private static final Logger LOGGER = LogManager.getLogger(SimpleAudioImageGenerator.class);

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generatePreview(File file, String extension) {
   
	ImejiFileIcon audioIcon = new ImejiFileIcon("audio_file_icon");
	try {
	    File copy = TempFileUtil.createTempFile("audio", extension);
	    FileUtils.copyFile(new File(SimpleAudioImageGenerator.class.getClassLoader()
	        .getResource(audioIcon.getIconPath()).toURI()), copy);
	    return copy;
	}
	catch (final URISyntaxException | IOException e) {
		LOGGER.error("Error creating thunmbnail", e);
	}

    return null;
  }

  @Override
  protected boolean generatorSupportsMimeType(String fileExtension) {
	boolean isAudio = StorageUtils.getMimeType(fileExtension).contains("audio");
	return isAudio;
  }
}
