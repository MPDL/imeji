package de.mpg.imeji.logic.storage.util;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

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
  public static File pdfToImage(File file) throws FileNotFoundException, IOException {
    PDDocument document = PDDocument.load(file);
    try {
      List<?> pages = document.getDocumentCatalog().getAllPages();
      PDPage page = (PDPage) pages.get(0); // first one
      BufferedImage bufferedImage = page.convertToImage();
      return ImageUtils.toFile(bufferedImage, StorageUtils.getMimeType("jpg"));
    } finally {
      document.close();
    }
  }

}
