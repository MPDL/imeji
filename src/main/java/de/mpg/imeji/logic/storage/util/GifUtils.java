/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
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
