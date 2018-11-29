package de.mpg.imeji.logic.storage.transform.generator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import javax.imageio.ImageIO;
import javax.imageio.stream.FileImageInputStream;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.transform.generator.icons.WriteOnIcon;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Generates a preview image of a file. Provides the opportunity to write customized text on a given
 * background file
 * 
 * @author breddin, original by saquet
 *
 */

public abstract class AbstractWritableImageGenerator extends ImageGenerator {

  /**
   * background image (jpg)
   */
  WriteOnIcon writeOnThisIcon;

  public AbstractWritableImageGenerator(String backgroundIconName, Point textPosition, Font font, Color textColor, int maxTextLength) {

    this.writeOnThisIcon = new WriteOnIcon(backgroundIconName, textPosition, font, textColor, maxTextLength);
  }

  /**
   * Use this function to access the background image, write text (file extension) on it and save it
   * 
   * @param file
   * @param textForImage
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  protected File generateAndWriteOnImage(File file, String textForImage) throws IOException, URISyntaxException {

    BufferedImage icon;
    icon = ImageIO.read(new FileImageInputStream(
        new File(AbstractWritableImageGenerator.class.getClassLoader().getResource(this.writeOnThisIcon.getIconPath()).toURI())));
    icon = writeTextOnImage(icon, textForImage);
    if (icon != null) {
      return ImageUtils.toFile(icon, StorageUtils.getMimeType("jpg"));
    }
    return null;

  }

  /**
   * Write the text on the background image ({@link BufferedImage})
   *
   * @param backgroundImage
   * @param textForImage
   * @return
   */
  private BufferedImage writeTextOnImage(BufferedImage backgroundImage, String textForImage) {
    if (StringHelper.isNullOrEmptyTrim(textForImage)) {
      return null;
    }
    final int iconWidth = backgroundImage.getWidth();
    final int iconHeight = backgroundImage.getHeight();
    final BufferedImage img = new BufferedImage(iconWidth, iconHeight, BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2d = img.createGraphics();
    g2d.drawImage(backgroundImage, 0, 0, null);
    g2d.setPaint(this.writeOnThisIcon.getTextColor());
    g2d.setFont(this.writeOnThisIcon.getTextFont());
    final FontMetrics fontMetrics = g2d.getFontMetrics();
    textForImage = formatText(textForImage);
    g2d.drawString(textForImage, this.writeOnThisIcon.getXPosition() - fontMetrics.stringWidth(textForImage),
        this.writeOnThisIcon.getYPosition());
    g2d.dispose();
    return img;
  }

  /**
   * Format the text to avoid to broken design
   *
   * @param extension
   * @return
   */
  private String formatText(String textForImage) {
    if (textForImage.length() > this.writeOnThisIcon.getMaxTextLength()) {
      textForImage = textForImage.substring(0, this.writeOnThisIcon.getMaxTextLength());
    }
    return textForImage.toUpperCase();
  }

}
