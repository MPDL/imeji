package de.mpg.imeji.test.logic.storage;

import java.io.File;
import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.logic.storage.util.PdfUtils;

public class PdfHandlingTest {

  @Test
  public void createImageFromPdfRenderer2Test() throws IOException {
    File file = new File("src/test/resources/pdf/pdfWith4Pages.pdf");
    File image = PdfUtils.pdfToImage(file);
    Assert.assertTrue(image != null && image.length() > 0);
  }
}
