package de.mpg.imeji.logic.storage.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.pdfbox.io.MemoryUsageSetting;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import de.mpg.imeji.logic.util.StorageUtils;

public final class PdfUtils {

	private PdfUtils() {
		// private constructor
	}

	/**
	 * Read a pdf File, et the first page, and return it as an image
	 *
	 * @param file
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	public static File pdfToImage(File file) throws IOException {
		final PDDocument document = PDDocument.load(file, MemoryUsageSetting.setupTempFileOnly());
		try {
			if (document.getNumberOfPages() > 0) {
				final PDFRenderer pdfRenderer = new PDFRenderer(document);
				final BufferedImage bim = pdfRenderer.renderImageWithDPI(0, 300, ImageType.RGB);
				return ImageUtils.toFile(bim, StorageUtils.getMimeType("jpg"));
			}
			return null;
		} finally {
			document.close();
		}
	}

}
