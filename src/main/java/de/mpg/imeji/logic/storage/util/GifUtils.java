package de.mpg.imeji.logic.storage.util;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.imageio.ImageIO;
import javax.imageio.stream.ImageOutputStream;

/**
 * Utility class for Gif images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class GifUtils {
  /**
   * Convert a gif to a jpeg
   *
   * @param bytes
   * @return
   * @throws Exception
   */
  public static byte[] toJPEG(byte[] bytes) throws Exception {
    return convert(bytes, Color.WHITE);
  }

  /**
   * Convert a gif to a jpeg and st the transparency of the jpeg to the passed color
   *
   * @param bytes
   * @param backgroundColor
   * @return
   * @throws Exception
   */
  private static byte[] convert(byte[] bytes, Color backgroundColor) throws Exception {
    final ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    final BufferedImage bufferedImage = ImageIO.read(inputStream);
    final BufferedImage newBi = new BufferedImage(bufferedImage.getWidth(),
        bufferedImage.getHeight(), BufferedImage.TYPE_INT_RGB);
    final Graphics2D g2d = (Graphics2D) newBi.getGraphics();
    g2d.drawImage(bufferedImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
        backgroundColor, null);
    final ByteArrayOutputStream osByteArray = new ByteArrayOutputStream();
    final ImageOutputStream outputStream = ImageIO.createImageOutputStream(osByteArray);
    try {
      ImageIO.write(newBi, "jpg", outputStream);
      return osByteArray.toByteArray();
    } finally {
      outputStream.flush();
      outputStream.close();
      osByteArray.close();
    }
  }
}
