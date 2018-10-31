package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.transform.generator.icons.ImejiFileIcon;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * {@link ImageGenerator} that returns a stored jpg icon file Different icons
 * files exist for different extensions (i.e. txt, ppt, etc)
 * 
 * @author jandura
 *
 */

public class TextFileIconGenerator extends ImageGenerator {

	private static final String[] TEXT_MIME_TYPES = {"text/plain", "text/comma-separated-values",
			"text/tab-separated-values", "application/msword", "application/mspowerpoint", "application/msexcel",
			"application/vnd.openxmlformats-officedocument.wordprocessingml.document",
			"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
			"application/vnd.openxmlformats-officedocument.presentationml.presentation",};

	private static final Logger LOGGER = LogManager.getLogger(RawFileImageGenerator.class);

	/**
	 * Icons for files of type "text" are stored in a separate subfolder
	 */
	private static final String SUBFOLDER_TEXT_FILE_ICONS = "icons/";

	@Override
	public File generatePreview(File file, String fileExtension) throws ImejiException {

		ImejiFileIcon textFileIcon = new ImejiFileIcon(SUBFOLDER_TEXT_FILE_ICONS + fileExtension);

		try {
			URL textIconResource = RawFileImageGenerator.class.getClassLoader().getResource(textFileIcon.getIconPath());
			if (textIconResource != null) {
				File tempIconFile = TempFileUtil.createTempFile("NiceRawFile",
						FilenameUtils.getExtension(new File(textIconResource.toURI()).getName()));
				FileUtils.copyFile(new File(textIconResource.toURI()), tempIconFile);

				if (tempIconFile.exists() && tempIconFile.length() > 0) {
					return tempIconFile;
				}
			}
			return null;
		} catch (URISyntaxException | IOException e) {
			LOGGER.error("Error creating icon for " + fileExtension, e);
			return null;
		}
	}

	@Override
	protected boolean generatorSupportsMimeType(String fileExtension) {

		String fileMimeType = StorageUtils.getMimeType(fileExtension);

		for (int i = 0; i < TEXT_MIME_TYPES.length; i++) {
			if (fileMimeType.equalsIgnoreCase(TEXT_MIME_TYPES[i])) {
				return true;
			}
		}
		return false;
	}

}
