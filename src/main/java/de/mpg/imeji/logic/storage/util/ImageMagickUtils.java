/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.storage.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.process.ProcessStarter;

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
public class ImageMagickUtils {
  private static final Logger LOGGER = Logger.getLogger(ImageMagickUtils.class);
  public static final boolean imageMagickEnabled = verifyImageMagickInstallation();

  /**
   * Return true if imagemagick is installed on the current system
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static boolean verifyImageMagickInstallation() {
    try {
      String imPath = getImageMagickInstallationPath();
      ConvertCmd cmd = new ConvertCmd(false);
      ProcessStarter.setGlobalSearchPath(imPath);
      cmd.setSearchPath(imPath);
      IMOperation op = new IMOperation();
      // get ImageMagick version
      op.version();
      cmd.run(op);
    } catch (Exception e) {
      LOGGER.error("imagemagick not installed", e);
      return false;
    }
    return true;
  }

  /**
   * User imagemagick to convert any image into a jpeg
   *
   * @param bytes
   * @param extension
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IM4JavaException
   */
  public static File convertToJPEG(File tmp, String extension)
      throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    // In case the file is made of many frames, (for instance videos), generate only the frames from
    // 0 to 48 to
    // avoid high memory consumption
    String path = tmp.getAbsolutePath() + "[0-48]";
    ConvertCmd cmd = getConvert();
    // create the operation, add images and operators/options
    IMOperation op = new IMOperation();
    if (isImage(extension)) {
      op.colorspace(findColorSpace(tmp));
    }
    op.strip();
    op.flatten();
    op.addImage(path);
    // op.colorspace("RGB");
    File jpeg = TempFileUtil.createTempFile("uploadMagick", ".jpg");
    try {
      op.addImage(jpeg.getAbsolutePath());
      cmd.run(op);
      int frame = getNonBlankFrame(jpeg.getAbsolutePath());
      if (frame >= 0) {
        File f = new File(FilenameUtils.getFullPath(jpeg.getAbsolutePath())
            + FilenameUtils.getBaseName(jpeg.getAbsolutePath()) + "-" + frame + ".jpg");
        return f;
      }
      return jpeg;
    } finally {
      removeFilesCreatedByImageMagick(jpeg.getAbsolutePath());
      FileUtils.deleteQuietly(jpeg);
    }
  }

  /**
   * Resize a gif. If animated, keep the animation
   *
   * @param file
   * @param resolution
   * @return
   */
  public static File resizeAnimatedGif(File file, FileResolution resolution) {
    try {
      if (!imageMagickEnabled) {
        return null;
      }
      String path = file.getAbsolutePath();
      ConvertCmd cmd = ImageMagickUtils.getConvert();
      // create the operation, add images and operators/options
      IMOperation op = new IMOperation();
      int size = getSize(file, resolution);
      if (resolution == FileResolution.THUMBNAIL) {
        op.thumbnail(size, size, "^");
      } else {
        op.thumbnail(size);
      }
      op.gravity("center");
      op.extent(size);
      op.addImage(path);
      File gif = TempFileUtil.createTempFile("uploadMagick", ".gif");
      try {
        op.addImage(gif.getAbsolutePath());
        cmd.run(op);
        return gif;
      } finally {
      }
    } catch (Exception e) {
      LOGGER.error("Error transforming gif", e);
    }
    return null;
  }

  /**
   * Get the Size if an image file
   *
   * @param file
   * @param resolution
   * @return
   * @throws Exception
   */
  private static int getSize(File file, FileResolution resolution) throws Exception {
    int size = ImageUtils.getResolution(resolution);
    Info info = ImageMagickUtils.getInfo(file);
    if (info.getImageWidth() > size || info.getImageHeight() > size) {
      return size;
    } else {
      return info.getImageWidth() > info.getImageHeight() ? info.getImageWidth()
          : info.getImageHeight();
    }
  }


  /**
   * True if the extension correspond to an image file
   *
   * @param extension
   * @return
   */
  private static boolean isImage(String extension) {
    return StorageUtils.getMimeType(extension).contains("image");
  }

  /**
   * Find the colorspace of the file
   *
   * @param tmp
   * @return
   * @throws IOException
   * @throws InterruptedException
   * @throws IM4JavaException
   * @throws URISyntaxException
   */
  public static String findColorSpace(File tmp) {
    try {
      Info imageInfo = new Info(tmp.getAbsolutePath());
      String cs = imageInfo.getProperty("Colorspace");
      if (cs != null) {
        return cs;
      }
    } catch (Exception e) {
      LOGGER.error("No color space found for " + tmp.getAbsolutePath(), e);
    }
    return "RGB";
  }

  /**
   * Return the info about the file as returned by imageMagick
   *
   * @param f
   * @return
   * @throws InfoException
   */
  public static Info getInfo(File f) throws InfoException {
    return new Info(f.getAbsolutePath(), true);
  }

  /**
   * Search for the first non blank image generated by imagemagick, based on commandline: convert
   * image.jpg -shave 1%x1% -resize 40% -fuzz 10% -trim +repage info: | grep ' 1x1 '
   *
   * @param path
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IM4JavaException
   */
  public static int getNonBlankFrame(String path)
      throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    ConvertCmd cmd = getConvert();
    int count = 0;
    String dir = FilenameUtils.getFullPath(path);
    String pathBase = FilenameUtils.getBaseName(path);
    File f = new File(dir + pathBase + "-" + count + ".jpg");
    while (f.exists()) {
      IMOperation op = new IMOperation();
      op.addImage();
      op.shave(1, 1, true);
      op.fuzz(10.0, true);
      op.trim();
      op.addImage();
      File trim = TempFileUtil.createTempFile("trim", ".jpg");
      try {
        cmd.run(op, f.getAbsolutePath(), trim.getAbsolutePath());
        Info info = new Info(trim.getAbsolutePath());
        if (!info.getImageGeometry().contains("1x1")) {
          return count;
        }
      } catch (Exception e) {
        LOGGER.info("Some problems with getting non blank frame!", e);
      } finally {
        String newPath = f.getAbsolutePath().replace("-" + count, "-" + Integer.valueOf(count + 1));
        f = new File(newPath);
        count++;
        trim.delete();
      }
    }
    return -1;
  }

  /**
   * Remove the files created by imagemagick.
   *
   * @param path
   */
  private static void removeFilesCreatedByImageMagick(String path) {
    int count = 0;
    String dir = FilenameUtils.getFullPath(path);
    String pathBase = FilenameUtils.getBaseName(path);
    File f = new File(dir + pathBase + "-" + count + ".jpg");
    while (f.exists()) {
      String newPath = f.getAbsolutePath().replace("-" + count, "-" + Integer.valueOf(count + 1));
      f.delete();
      f = new File(newPath);
      count++;
    }
  }

  /**
   * Create a {@link ConvertCmd}
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static ConvertCmd getConvert() throws IOException, URISyntaxException {
    String magickPath = getImageMagickInstallationPath();
    ConvertCmd cmd = new ConvertCmd(false);
    cmd.setSearchPath(magickPath);
    return cmd;
  }

  /**
   * Return property imeji.imagemagick.installpath
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  private static String getImageMagickInstallationPath() throws IOException, URISyntaxException {
    return PropertyReader.getProperty("imeji.imagemagick.installpath");
  }
}
