/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.storage.util;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Iterator;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Mehtods to help wotk with images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class ImageUtils {
  private static final Logger LOGGER = Logger.getLogger(ImageUtils.class);
  /**
   * If true, the rescale will keep the better quality of the images
   */
  private static final boolean RESCALE_HIGH_QUALITY = true;

  private ImageUtils() {
    // private constructor
  }

  /**
   * Resize an image (only for jpeg) to the given {@link FileResolution}
   *
   * @param bytes
   * @param resolution
   * @return
   * @throws IOException
   * @throws Exception
   */
  public static File resizeJPEG(File file, FileResolution resolution)
      throws IOException, Exception {
    // If it is original resolution, don't touch the file, otherwise resize
    if (!FileResolution.ORIGINAL.equals(resolution)) {
      BufferedImage image = JpegUtils.readJpeg(file);
      return toFile(scaleImage(image, resolution), StorageUtils.getMimeType("jpg"));
    }
    return file;
  }

  /**
   * Transform an image in jpeg. Useful to reduce size of thumbnail and web resolution images. If
   * the format of the image is not supported, return null
   *
   * @param bytes
   * @param mimeType
   * @return
   */
  public static byte[] toJpeg(byte[] bytes, String mimeType) {
    try {
      if (mimeType.equals(StorageUtils.getMimeType("jpg"))) {
        return bytes;
      } else if (mimeType.equals(StorageUtils.getMimeType("gif"))) {
        return GifUtils.toJPEG(bytes);
      } else {
        return image2Jpeg(bytes);
      }
    } catch (Exception e) {
      LOGGER.info("Image could not be transformed to jpeg: ", e);
    }
    return new byte[0];
  }

  public static File toJpeg(File file, String mimeType) {
    try {
      if (mimeType.equals(StorageUtils.getMimeType("jpg"))) {
        return file;
      } else if (mimeType.equals(StorageUtils.getMimeType("gif"))) {
        return StorageUtils
            .toFile(GifUtils.toJPEG(StorageUtils.toBytes(new FileInputStream(file))));
      } else {
        return image2Jpeg(file);
      }
    } catch (Exception e) {
      LOGGER.info("Image could not be transformed to jpeg: ", e);
    }
    return null;
  }

  /**
   * Scale a {@link BufferedImage} to new size. Is faster than the basic {@link ImageUtils}
   * .scaleImage method, has the same quality. If it is a thumbnail, cut the images to fit into the
   * raster
   *
   * @param image original image
   * @param size the size to be resized to
   * @param resolution the type of the image. Might be thumb or web
   * @return the resized images
   * @throws Exception
   */
  private static BufferedImage scaleImageFast(BufferedImage image, int size,
      FileResolution resolution) throws Exception {
    int width = image.getWidth(null);
    int height = image.getHeight(null);
    BufferedImage newImg = null;
    Image rescaledImage;
    int colorSpace = BufferedImage.TYPE_INT_RGB;
    if (width > height) {
      if (FileResolution.THUMBNAIL.equals(resolution)) {
        newImg = new BufferedImage(height, height, colorSpace);
        Graphics g1 = newImg.createGraphics();
        g1.drawImage(image, (height - width) / 2, 0, null);
        if (height > size) {
          rescaledImage = getScaledInstance(newImg, size, size,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
        } else {
          rescaledImage = newImg;
        }
      } else {
        rescaledImage = getScaledInstance(image, size, height * size / width,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
      }
    } else {
      if (FileResolution.THUMBNAIL.equals(resolution)) {
        newImg = new BufferedImage(width, width, colorSpace);
        Graphics g1 = newImg.createGraphics();
        g1.drawImage(image, 0, (width - height) / 2, null);
        if (width > size) {
          rescaledImage = getScaledInstance(newImg, size, size,
              RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
        } else {
          rescaledImage = newImg;
        }
      } else {
        rescaledImage = getScaledInstance(image, width * size / height, size,
            RenderingHints.VALUE_INTERPOLATION_BILINEAR, RESCALE_HIGH_QUALITY);
      }
    }
    BufferedImage rescaledBufferedImage =
        new BufferedImage(rescaledImage.getWidth(null), rescaledImage.getHeight(null), colorSpace);
    Graphics g2 = rescaledBufferedImage.getGraphics();
    g2.drawImage(rescaledImage, 0, 0, null);
    return rescaledBufferedImage;
  }

  /**
   * Convenience method that returns a scaled instance of the provided {@link BufferedImage}.
   *
   * @param img the original image to be scaled
   * @param targetWidth the desired width of the scaled instance, in pixels
   * @param targetHeight the desired height of the scaled instance, in pixels
   * @param hint one of the rendering hints that corresponds to
   *        {@code RenderingHints.KEY_INTERPOLATION} (e.g.
   *        {@code RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR},
   *        {@code RenderingHints.VALUE_INTERPOLATION_BILINEAR},
   *        {@code RenderingHints.VALUE_INTERPOLATION_BICUBIC})
   * @param higherQuality if true, this method will use a multi-step scaling technique that provides
   *        higher quality than the usual one-step technique (only useful in downscaling cases,
   *        where {@code targetWidth} or {@code targetHeight} is smaller than the original
   *        dimensions, and generally only when the {@code BILINEAR} hint is specified)
   * @return a scaled version of the original {@link BufferedImage}
   */
  private static BufferedImage getScaledInstance(BufferedImage img, int targetWidth,
      int targetHeight, Object hint, boolean higherQuality) {
    int type = (img.getTransparency() == Transparency.OPAQUE) ? BufferedImage.TYPE_INT_RGB
        : BufferedImage.TYPE_INT_ARGB;
    BufferedImage ret = img;
    int w, h;
    if (higherQuality) {
      // Use multi-step technique: start with original size, then
      // scale down in multiple passes with drawImage()
      // until the target size is reached
      w = img.getWidth();
      h = img.getHeight();
    } else {
      // Use one-step technique: scale directly from original
      // size to target size with a single drawImage() call
      w = targetWidth;
      h = targetHeight;
    }
    do {
      if (higherQuality && w > targetWidth) {
        w /= 2;
        if (w < targetWidth) {
          w = targetWidth;
        }
      }
      if (higherQuality && h > targetHeight) {
        h /= 2;
        if (h < targetHeight) {
          h = targetHeight;
        }
      }
      BufferedImage tmp = new BufferedImage(w, h, type);
      Graphics2D g2 = tmp.createGraphics();
      g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, hint);
      g2.drawImage(ret, 0, 0, w, h, null);
      g2.dispose();
      ret = tmp;
    } while (w != targetWidth || h != targetHeight);
    return ret;
  }

  /**
   * Transform a image to a jpeg image. The input image must have a format supported by
   * {@link ImageIO}
   *
   * @param bytes
   * @return
   * @throws IOException
   */
  private static byte[] image2Jpeg(byte[] bytes) throws IOException {
    InputStream ins = new ByteArrayInputStream(bytes);
    BufferedImage image = ImageIO.read(ins);
    ByteArrayOutputStream ous = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", ous);
    return ous.toByteArray();
  }


  /**
   * Transform a image to a jpeg image. The input image must have a format supported by
   * {@link ImageIO}
   *
   * @param File
   * @return
   * @throws IOException
   */
  private static File image2Jpeg(File file) throws IOException {
    BufferedImage image = ImageIO.read(new FileInputStream(file));
    return toFile(image, StorageUtils.getMimeType("jpg"));
  }

  /**
   * Return the format of an image (jpg, tif), according to its mime-type
   *
   * @param mimeType
   * @return
   */
  public static String getImageFormat(String mimeType) {
    if (mimeType.equals(StorageUtils.getMimeType("tif"))) {
      return "tif";
    }
    return mimeType.toLowerCase().replaceAll("image/", "");
  }

  /**
   * TRansform a {@link BufferedImage} to a {@link Byte} array
   *
   * @param image
   * @param mimeType
   * @return
   * @throws IOException
   */
  public static byte[] toBytes(BufferedImage image, String mimeType) throws IOException {
    ByteArrayOutputStream byteOutput = new ByteArrayOutputStream();
    ImageIO.write(image, ImageUtils.getImageFormat(mimeType), byteOutput);
    return byteOutput.toByteArray();
  }

  /**
   * Wrinte a {@link BufferedImage} into a File
   *
   * @param image
   * @param mimeType
   * @return
   * @throws IOException
   */
  public static File toFile(BufferedImage image, String mimeType) throws IOException {
    File file = TempFileUtil.createTempFile("ImageUtils_toFile", null);
    FileOutputStream fos = new FileOutputStream(file);
    ImageIO.write(image, ImageUtils.getImageFormat(mimeType), fos);
    return file;
  }

  /**
   * cale the image if too big for the size
   *
   * @param image
   * @param resolution
   * @return
   * @throws Exception
   */
  public static BufferedImage scaleImage(BufferedImage image, FileResolution resolution)
      throws Exception {
    BufferedImage bufferedImage = null;
    int size = getResolution(resolution);
    if (image.getWidth() > size || image.getHeight() > size) {
      bufferedImage = scaleImageFast(image, size, resolution);
    } else {
      size = image.getWidth() > image.getHeight() ? image.getWidth() : image.getHeight();
      bufferedImage = scaleImageFast(image, size, resolution);
    }
    return bufferedImage;
  }

  /**
   * Return the maximum size of an image according to its {@link FileResolution}. The values are
   * defined in the properties
   *
   * @param FileResolution
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static int getResolution(FileResolution resolution)
      throws IOException, URISyntaxException {
    switch (resolution) {
      case THUMBNAIL:
        return Integer.parseInt(PropertyReader.getProperty("xsd.resolution.thumbnail"));
      case WEB:
        return Integer.parseInt(PropertyReader.getProperty("xsd.resolution.web"));
      default:
        return 0;
    }
  }

  /**
   * Return the property xsd.metadata.content-category.thumbnail
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static String getEscidocThumbnailContentCategory() throws IOException, URISyntaxException {
    return PropertyReader.getProperty("xsd.metadata.content-category.thumbnail");
  }

  /**
   * Return the property xsd.metadata.content-category.web-resolution
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static String getEscidocWebContentCategory() throws IOException, URISyntaxException {
    return PropertyReader.getProperty("xsd.metadata.content-category.web-resolution");
  }

  /**
   * Return the property xsd.metadata.content-category.original-resolution
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static String getEscidocOriginalContentCategory() throws IOException, URISyntaxException {
    return PropertyReader.getProperty("xsd.metadata.content-category.original-resolution");
  }

  public static Dimension getImageDimension(File f) {
    try (ImageInputStream in = ImageIO.createImageInputStream(f)) {
      final Iterator<ImageReader> readers = ImageIO.getImageReaders(in);
      if (readers.hasNext()) {
        ImageReader reader = readers.next();
        try {
          reader.setInput(in);
          return new Dimension(reader.getWidth(0), reader.getHeight(0));
        } finally {
          reader.dispose();
        }
      }
    } catch (Exception e) {
      LOGGER.error("Error reading image dimension: ", e);
    }
    return null;
  }
}
