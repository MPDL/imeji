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
import java.io.IOException;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.metadata.IIOMetadataNode;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import de.mpg.imeji.logic.storage.Storage.FileResolution;

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
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    BufferedImage bufferedImage = ImageIO.read(inputStream);
    BufferedImage newBi = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    Graphics2D g2d = (Graphics2D) newBi.getGraphics();
    g2d.drawImage(bufferedImage, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(),
        backgroundColor, null);
    ByteArrayOutputStream osByteArray = new ByteArrayOutputStream();
    ImageOutputStream outputStream = ImageIO.createImageOutputStream(osByteArray);
    try {
      ImageIO.write(newBi, "jpg", outputStream);
      return osByteArray.toByteArray();
    } finally {
      outputStream.flush();
      outputStream.close();
      osByteArray.close();
    }
  }

  /**
   * Resize an animated gif: read all frames and resize each of this frame, and create a new gif
   * with this new resized frames
   * 
   * @param bytes
   * @param resolution
   * @return
   * @throws Exception
   */
  public static byte[] resizeAnimatedGif(byte[] bytes, FileResolution resolution) throws Exception {
    ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
    ImageInputStream iis = ImageIO.createImageInputStream(inputStream);
    Iterator<ImageReader> readers = ImageIO.getImageReadersBySuffix("gif");
    while (readers.hasNext()) {
      ImageReader reader = readers.next();
      reader.setInput(iis);
      int numberOfImages = reader.getNumImages(true);
      int minIndex = reader.getMinIndex();
      AnimatedGifEncoder gifEncoder = new AnimatedGifEncoder();
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      gifEncoder.start(bos);
      int delay = getAnimationDelay(reader, minIndex) * 10; // * 10 because seems to be correct...
      gifEncoder.setDelay(delay);
      gifEncoder.setRepeat(0);
      for (int i = minIndex; i < numberOfImages; i++) {
        BufferedImage bi = reader.read(i);
        if (bi != null) {
          bi = ImageUtils.scaleImage(bi, resolution);
          gifEncoder.addFrame(bi);
        }
      }
      bytes = bos.toByteArray();
      gifEncoder.finish();
      return bytes;
    }
    return bytes;
  }

  /**
   * Check if a an image is a gif and if is contains more than one image (i.e is animated)
   * 
   * @param bytes
   * @return
   */
  public static boolean isAnimatedGif(byte[] bytes) {
    ImageReader reader = ImageIO.getImageReadersBySuffix("gif").next();
    try {
      reader.setInput(ImageIO.createImageInputStream(new ByteArrayInputStream(bytes)));
      return reader.getNumImages(true) > 1;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Return the delay of the animation
   * 
   * @param reader
   * @param minIndex
   * @return
   * @throws IOException
   */
  private static int getAnimationDelay(ImageReader reader, int minIndex) throws IOException {
    IIOImage iioi = reader.readAll(minIndex, null);
    IIOMetadata md = iioi.getMetadata();
    IIOMetadataNode imgRootNode = null;
    try {
      imgRootNode = (IIOMetadataNode) md.getAsTree("javax_imageio_gif_image_1.0");
    } catch (IllegalArgumentException e) {
      // unkown metadata format, can't do anyting about this
      return 10;
    }
    IIOMetadataNode gce =
        (IIOMetadataNode) imgRootNode.getElementsByTagName("GraphicControlExtension").item(0);
    return Integer.parseInt(gce.getAttribute("delayTime"));
  }
}
