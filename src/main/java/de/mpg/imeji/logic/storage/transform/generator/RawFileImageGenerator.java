package de.mpg.imeji.logic.storage.transform.generator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * {@link ImageGenerator} for all unknown/unsupported format. It creates a default image
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class RawFileImageGenerator implements ImageGenerator {
  private static final Logger LOGGER = Logger.getLogger(RawFileImageGenerator.class);
  private static final String PATH_TO_DEFAULT_IMAGE = "images/file-icon.jpg";
  /**
   * Coordinates where the text is written on the image
   */
  private static final int TEXT_POSITION_X = 630;
  private static final int TEXT_POSITION_Y = 700;
  /**
   * The Font size of the text
   */
  private static final int TEXT_FONT_SIZE = 150;
  private static final Font FONT = new Font("Serif", Font.BOLD, TEXT_FONT_SIZE);

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.transform.ImageGenerator#generateJPG(byte[], java.lang.String)
   */
  @Override
  public File generateJPG(File file, String extension) {
    BufferedImage icon;
    try {
      icon = ImageIO.read(new FileImageInputStream(new File(RawFileImageGenerator.class
          .getClassLoader().getResource(PATH_TO_DEFAULT_IMAGE).toURI())));
      System.out.println("EEEEEEEEEEEE " + extension);
      icon = writeTextOnImage(icon, extension, file.getName());
      return ImageUtils.toFile(icon, StorageUtils.getMimeType("jpg"));
    } catch (IOException | URISyntaxException e) {
      LOGGER.error("Error creating icon", e);
    }
    return null;
  }

  /**
   * Write the extension on the {@link BufferedImage}
   *
   * @param old
   * @param extension
   * @return
   */
  private BufferedImage writeTextOnImage(BufferedImage old, String extension, String fileName) {
    final int w = old.getWidth();
    final int h = old.getHeight();
    final BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2d = img.createGraphics();
    g2d.drawImage(old, 0, 0, null);
    g2d.setPaint(Color.WHITE);
    g2d.setFont(FONT);
    final FontMetrics fm = g2d.getFontMetrics();
    if (StringHelper.isNullOrEmptyTrim(extension)) {
      extension = FilenameUtils.getExtension(fileName);
    }
    extension = formatExtension(extension);
    g2d.drawString(extension, TEXT_POSITION_X - fm.stringWidth(extension), TEXT_POSITION_Y);
    g2d.dispose();
    return img;
  }

  /**
   * Format the extension to avoid to broke the design of the created icon
   *
   * @param extension
   * @return
   */
  private String formatExtension(String extension) {
    if (extension.length() > 3) {
      extension = extension.substring(0, 3);
    }
    return extension.toUpperCase();
  }
}
