package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;
import java.io.IOException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.storage.util.PdfUtils;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * {@link ImageGenerator} to generate image out of pdf
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class PdfImageGenerator extends ImageGenerator {
	private static final Logger LOGGER = LogManager.getLogger(PdfImageGenerator.class);

	/*
	 * (non-Javadoc)
	 *
	 * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[],
	 * java.lang.String)
	 */
	@Override
	public File generatePreview(File file, String extension) {

		try {
			return PdfUtils.pdfToImage(file);
		} catch (final IOException e) {
			LOGGER.error("Error reading pdf file", e);
		}
		return null;
	}

	@Override
	protected boolean generatorSupportsMimeType(String fileExtension) {
		boolean isPdfFormat = StorageUtils.getMimeType(fileExtension).equals("application/pdf");
		return isPdfFormat;
	}
}
