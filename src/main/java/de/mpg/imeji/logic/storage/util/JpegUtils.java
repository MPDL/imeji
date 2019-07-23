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
import java.util.List;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.commons.imaging.ImageReadException;
import org.apache.commons.imaging.Imaging;
import org.apache.commons.imaging.common.bytesource.ByteSource;
import org.apache.commons.imaging.common.bytesource.ByteSourceFile;
import org.apache.commons.imaging.formats.jpeg.JpegImageParser;
import org.apache.commons.imaging.formats.jpeg.segments.Segment;
import org.apache.commons.imaging.formats.jpeg.segments.UnknownSegment;
import org.apache.commons.io.FileUtils;

import de.mpg.imeji.logic.util.StorageUtils;
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
    final File f = TempFileUtil.createTempFile("JpegUtils_readjpg", ".jpg");
    try {
      StorageUtils.writeInOut(new ByteArrayInputStream(bytes), new FileOutputStream(f), true);
      final BufferedImage bi = readJpeg(f);
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
    final ImageInputStream stream = ImageIO.createImageInputStream(file);
    final Iterator<ImageReader> iter = ImageIO.getImageReaders(stream);
    try {
      while (iter.hasNext()) {
        final ImageReader reader = iter.next();
        reader.setInput(stream);
        BufferedImage image = null;
        ICC_Profile profile = null;
        try {
          image = reader.read(0);
        } catch (final IIOException e) {
          colorType = COLOR_TYPE_CMYK;
          checkAdobeMarker(file);
          profile = Imaging.getICCProfile(file);
          final WritableRaster raster = (WritableRaster) reader.readRaster(0, null);
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
    final JpegImageParser parser = new JpegImageParser();
    final ByteSource byteSource = new ByteSourceFile(file);
    @SuppressWarnings("rawtypes")
    final List<Segment> segments = parser.readSegments(byteSource, new int[] {0xffee}, true);
    if (segments != null && segments.size() >= 1) {
      final UnknownSegment app14Segment = (UnknownSegment) segments.get(0);
      final byte[] data = app14Segment.getSegmentData();
      if (data.length >= 12 && data[0] == 'A' && data[1] == 'd' && data[2] == 'o' && data[3] == 'b' && data[4] == 'e') {
        hasAdobeMarker = true;
        final int transform = app14Segment.getSegmentData()[11] & 0xff;
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
    final int height = raster.getHeight();
    final int width = raster.getWidth();
    final int stride = width * 4;
    final int[] pixelRow = new int[stride];
    for (int h = 0; h < height; h++) {
      raster.getPixels(0, h, width, 1, pixelRow);
      for (int x = 0; x < stride; x += 4) {
        int y = pixelRow[x];
        final int cb = pixelRow[x + 1];
        final int cr = pixelRow[x + 2];
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
    final int height = raster.getHeight();
    final int width = raster.getWidth();
    final int stride = width * 4;
    final int[] pixelRow = new int[stride];
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
  private static BufferedImage convertCmykToRgb(Raster cmykRaster, ICC_Profile cmykProfile) throws IOException {
    if (cmykProfile == null) {
      cmykProfile = ICC_Profile.getInstance(JpegUtils.class.getResourceAsStream("/ISOcoated_v2_300_eci.icc"));
    }
    if (cmykProfile.getProfileClass() != ICC_Profile.CLASS_DISPLAY) {
      final byte[] profileData = cmykProfile.getData();
      if (profileData[ICC_Profile.icHdrRenderingIntent] == ICC_Profile.icPerceptual) {
        intToBigEndian(ICC_Profile.icSigDisplayClass, profileData, ICC_Profile.icHdrDeviceClass);
        cmykProfile = ICC_Profile.getInstance(profileData);
      }
    }
    final ICC_ColorSpace cmykCS = new ICC_ColorSpace(cmykProfile);
    final BufferedImage rgbImage = new BufferedImage(cmykRaster.getWidth(), cmykRaster.getHeight(), BufferedImage.TYPE_INT_RGB);
    final WritableRaster rgbRaster = rgbImage.getRaster();
    final ColorSpace rgbCS = rgbImage.getColorModel().getColorSpace();
    final ColorConvertOp cmykToRgb = new ColorConvertOp(cmykCS, rgbCS, null);
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
