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

import java.awt.color.ColorSpace;
import java.awt.color.ICC_ColorSpace;
import java.awt.color.ICC_Profile;
import java.awt.image.BufferedImage;
import java.awt.image.ColorConvertOp;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.io.FileUtils;
import org.apache.sanselan.ImageReadException;
import org.apache.sanselan.Sanselan;
import org.apache.sanselan.common.byteSources.ByteSource;
import org.apache.sanselan.common.byteSources.ByteSourceFile;
import org.apache.sanselan.formats.jpeg.JpegImageParser;
import org.apache.sanselan.formats.jpeg.segments.UnknownSegment;

import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Utility class to read Jpeg images. This allow to read CMYK images
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class JpegUtils {
  public static final int COLOR_TYPE_RGB = 1;
  public static final int COLOR_TYPE_CMYK = 2;
  public static final int COLOR_TYPE_YCCK = 3;
  private static int colorType = COLOR_TYPE_RGB;
  private static boolean hasAdobeMarker = false;

  /**
   * Read a {@link Byte} array and transform it to a {@link BufferedImage}. The input must by an
   * image file
   * 
   * @param bytes
   * @return
   * @throws IOException
   * @throws ImageReadException
   */
  public static BufferedImage readJpeg(byte[] bytes) throws IOException, ImageReadException {
    File f = TempFileUtil.createTempFile("JpegUtils_readjpg", ".jpg");
    try {
      StorageUtils.writeInOut(new ByteArrayInputStream(bytes), new FileOutputStream(f), true);
      BufferedImage bi = readJpeg(f);
      return bi;
    } finally {
      FileUtils.deleteQuietly(f);
    }
  }

  /**
   * Read a {@link File} and transform it to a {@link BufferedImage}. The input must by an image
   * file
   * 
   * @param file
   * @return
   * @throws IOException
   * @throws ImageReadException
   */
  public static BufferedImage readJpeg(File file) throws IOException, ImageReadException {
    colorType = COLOR_TYPE_RGB;
    hasAdobeMarker = false;
    ImageInputStream stream = ImageIO.createImageInputStream(file);
    Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
    try {
      while (iter.hasNext()) {
        ImageReader reader = iter.next();
        reader.setInput(stream);
        BufferedImage image = null;
        ICC_Profile profile = null;
        try {
          image = reader.read(0);
        } catch (IIOException e) {
          colorType = COLOR_TYPE_CMYK;
          checkAdobeMarker(file);
          profile = Sanselan.getICCProfile(file);
          WritableRaster raster = (WritableRaster) reader.readRaster(0, null);
          if (colorType == COLOR_TYPE_YCCK) {
            convertYcckToCmyk(raster);
          }
          if (hasAdobeMarker) {
            convertInvertedColors(raster);
          }
          image = convertCmykToRgb(raster, profile);
        }
        return image;
      }
    } finally {
      stream.close();
    }
    return null;
  }

  /**
   * @param file
   * @throws IOException
   * @throws ImageReadException
   */
  private static void checkAdobeMarker(File file) throws IOException, ImageReadException {
    JpegImageParser parser = new JpegImageParser();
    ByteSource byteSource = new ByteSourceFile(file);
    @SuppressWarnings("rawtypes")
    ArrayList segments = parser.readSegments(byteSource, new int[] {0xffee}, true);
    if (segments != null && segments.size() >= 1) {
      UnknownSegment app14Segment = (UnknownSegment) segments.get(0);
      byte[] data = app14Segment.bytes;
      if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' && data[2] == 'o' && data[3] == 'b'
          && data[4] == 'e') {
        hasAdobeMarker = true;
        int transform = app14Segment.bytes[11] & 0xff;
        if (transform == 2) {
          colorType = COLOR_TYPE_YCCK;
        }
      }
    }
  }

  /**
   * @param raster
   */
  private static void convertYcckToCmyk(WritableRaster raster) {
    int height = raster.getHeight();
    int width = raster.getWidth();
    int stride = width * 4;
    int[] pixelRow = new int[stride];
    for (int h = 0; h < height; h++) {
      raster.getPixels(0, h, width, 1, pixelRow);
      for (int x = 0; x < stride; x += 4) {
        int y = pixelRow[x];
        int cb = pixelRow[x + 1];
        int cr = pixelRow[x + 2];
        int c = (int) (y + 1.402 * cr - 178.956);
        int m = (int) (y - 0.34414 * cb - 0.71414 * cr + 135.95984);
        y = (int) (y + 1.772 * cb - 226.316);
        if (c < 0) {
          c = 0;
        } else if (c > 255) {
          c = 255;
        }
        if (m < 0) {
          m = 0;
        } else if (m > 255) {
          m = 255;
        }
        if (y < 0) {
          y = 0;
        } else if (y > 255) {
          y = 255;
        }
        pixelRow[x] = 255 - c;
        pixelRow[x + 1] = 255 - m;
        pixelRow[x + 2] = 255 - y;
      }
      raster.setPixels(0, h, width, 1, pixelRow);
    }
  }

  /**
   * @param raster
   */
  private static void convertInvertedColors(WritableRaster raster) {
    int height = raster.getHeight();
    int width = raster.getWidth();
    int stride = width * 4;
    int[] pixelRow = new int[stride];
    for (int h = 0; h < height; h++) {
      raster.getPixels(0, h, width, 1, pixelRow);
      for (int x = 0; x < stride; x++) {
        pixelRow[x] = 255 - pixelRow[x];
      }
      raster.setPixels(0, h, width, 1, pixelRow);
    }
  }

  /**
   * @param cmykRaster
   * @param cmykProfile
   * @return
   * @throws IOException
   */
  private static BufferedImage convertCmykToRgb(Raster cmykRaster, ICC_Profile cmykProfile)
      throws IOException {
    if (cmykProfile == null) {
      cmykProfile =
          ICC_Profile.getInstance(JpegUtils.class.getResourceAsStream("/ISOcoated_v2_300_eci.icc"));
    }
    if (cmykProfile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
      byte[] profileData = cmykProfile.getData();
      if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
        intToBigEndian(ICC_Profile.icSigDisplayClass, profileData, ICC_Profile.icHdrDeviceClass);
        cmykProfile = ICC_Profile.getInstance(profileData);
      }
    }
    ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
    BufferedImage rgbImage = new BufferedImage(cmykRaster.getWidth(), cmykRaster.getHeight(),
        BufferedImage.TYPE_INT_RGB);
    WritableRaster rgbRaster = rgbImage.getRaster();
    ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
    ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
    cmykToRgb.filter(cmykRaster, rgbRaster);
    return rgbImage;
  }

  /**
   * @param value
   * @param array
   * @param index
   */
  private static void intToBigEndian(int value, byte[] array, int index) {
    array[index] = (byte) (value >> 24);
    array[index + 1] = (byte) (value >> 16);
    array[index + 2] = (byte) (value >> 8);
    array[index + 3] = (byte) (value);
  }
}
