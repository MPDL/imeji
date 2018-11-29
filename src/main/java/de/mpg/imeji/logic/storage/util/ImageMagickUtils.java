package de.mpg.imeji.logic.storage.util;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import org.im4java.core.ConvertCmd;
import org.im4java.core.IM4JavaException;
import org.im4java.core.IMOperation;
import org.im4java.core.Info;
import org.im4java.core.InfoException;
import org.im4java.core.JpegtranCmd;
import org.im4java.core.MogrifyCmd;
import org.im4java.process.ProcessStarter;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Methods to help work with images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImageMagickUtils {
  private static final Logger LOGGER = LogManager.getLogger(ImageMagickUtils.class);
  public static final boolean imageMagickEnabled = verifyImageMagickInstallation();
  public static final boolean jpegtranEnabled = false;

  /**
   * Return true if imagemagick is installed on the current system
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static boolean verifyImageMagickInstallation() {
    try {
      final String imageMagickPath = getImageMagickInstallationPath();
      final ConvertCmd convertCommand = new ConvertCmd(false);
      ProcessStarter.setGlobalSearchPath(imageMagickPath);
      convertCommand.setSearchPath(imageMagickPath);
      final IMOperation imageMagickOperationFromCommandline = new IMOperation();
      // get ImageMagick version
      imageMagickOperationFromCommandline.version();
      convertCommand.run(imageMagickOperationFromCommandline);
    } catch (final Exception e) {
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
    // Image Magic can't convert from tiff to jpg directly, thus use workaround and
    // first convert to
    // bmp, than to jpg
    if (extension.equals("tif") || extension.equals("tiff")) {
      File bmp = convert(tmp, extension, ".bmp");
      return convert(bmp, ".bmp", ".jpg");
    }
    return convert(tmp, extension, ".jpg");
  }

  /**
   * Resize a jpg to another size, defined by the resolution
   * 
   * @param file
   * @param extension
   * @param resolution
   * @return
   */
  public static File resizeJpg(File file, String extension, FileResolution resolution) {
    try {
      switch (resolution) {
        case ORIGINAL:
          return file;
        case FULL:
          return StorageUtils.compareExtension(extension, "jpg") ? file : convertToJPEG(file, extension);
        case WEB:
          return resize(file, extension, ImageUtils.getResolution(FileResolution.WEB));
        case THUMBNAIL:
          return cropCenter(file, extension, ImageUtils.getResolution(FileResolution.THUMBNAIL));
      }
    } catch (Exception e) {
      LOGGER.error("Error resizing jpg with Imagemagick for file " + file.getAbsolutePath() + " with extension " + extension, e);
    }
    return null;
  }

  /**
   * Resize a file as a jpeg
   * 
   * @param file
   * @param extension
   * @return
   * @throws IOException
   * @throws InterruptedException
   * @throws IM4JavaException
   * @throws URISyntaxException
   */
  private static File resize(File file, String extension, int resolution)
      throws IOException, InterruptedException, IM4JavaException, URISyntaxException {
    final File resized = TempFileUtil.createTempFile("magickResize", ".jpg");
    final ConvertCmd cmd = getConvert();
    IMOperation op = new IMOperation();
    if (!isImage(extension)) {
      file = convertToJPEG(file, extension);
    }
    op.quality(80.0);
    op.thumbnail(resolution, resolution, "");
    op.addImage(file.getAbsolutePath());
    op.addImage(resized.getAbsolutePath());
    cmd.run(op);
    return resized;
  }

  /**
   * Crop in the middle a File as a jpg
   * 
   * @param file
   * @param extension
   * @param resolution
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IM4JavaException
   */
  private static File cropCenter(File file, String extension, int resolution)
      throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    final File resized = TempFileUtil.createTempFile("magickCrop", ".jpg");
    final ConvertCmd cmd = getConvert();
    IMOperation op = new IMOperation();
    if (!isImage(extension)) {
      file = convertToJPEG(file, extension);
    }
    op.quality(80.0);
    op.thumbnail(resolution, resolution, "^");
    op.gravity("center");
    op.extent(resolution, resolution);
    op.addImage(file.getAbsolutePath());
    op.addImage(resized.getAbsolutePath());
    cmd.run(op);
    return resized;
  }

  /**
   * Convert a File from a file format (extensionIn) to another File format (extensionOut)
   * 
   * @param tmp
   * @param extensionIn
   * @param extensionOut
   * @return
   * @throws IOException
   * @throws URISyntaxException
   * @throws InterruptedException
   * @throws IM4JavaException
   */
  private static File convert(File tmp, String extensionIn, String extensionOut)
      throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    // if (!FilenameUtils.getExtension(tmp.getName()).equals(extensionIn)) {
    // File tmpWithOrginalExtension = new File(tmp.getAbsolutePath() + "." +
    // extensionIn);
    // tmp.renameTo(tmpWithOrginalExtension);
    // tmp = tmpWithOrginalExtension;
    // }
    // In case the file is made of many frames, (for instance videos), generate only
    // the frames from
    // 0 to 24 to avoid high memory consumption
    String path = tmp.getAbsolutePath() + "[0-24]";
    final ConvertCmd cmd = getConvert();
    // create the operation, add images and operators/options
    IMOperation op = new IMOperation();
    if (!isImage(extensionIn) && !isVideo(extensionIn)) {
      return null;
    }
    op.flatten();
    op.strip();
    op.quality(70.0);
    op.addImage(path);
    final File jpeg = TempFileUtil.createTempFile("uploadMagick", extensionOut);
    try {
      op.addImage(jpeg.getAbsolutePath());
      cmd.run(op);
      final int frame = getNonBlankFrame(jpeg.getAbsolutePath());
      if (frame >= 0) {
        final File f = new File(FilenameUtils.getFullPath(jpeg.getAbsolutePath()) + FilenameUtils.getBaseName(jpeg.getAbsolutePath()) + "-"
            + frame + extensionOut);
        return f;
      }
      return jpeg;
    } finally {
      removeFilesCreatedByImageMagick(jpeg.getAbsolutePath());
      // FileUtils.deleteQuietly(jpeg);
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
      final String path = file.getAbsolutePath();
      final ConvertCmd cmd = ImageMagickUtils.getConvert();
      // create the operation, add images and operators/options
      final IMOperation op = new IMOperation();
      final int size = getSize(file, resolution);
      if (resolution == FileResolution.THUMBNAIL) {
        op.thumbnail(size, size, "^");
      } else {
        op.thumbnail(size);
      }
      op.gravity("center");
      op.extent(size);
      op.addImage(path);
      final File gif = TempFileUtil.createTempFile("uploadMagick", ".gif");
      try {
        op.addImage(gif.getAbsolutePath());
        cmd.run(op);
        return gif;
      } finally {
      }
    } catch (final Exception e) {
      LOGGER.error("Error transforming gif", e);
    }
    return null;
  }

  /**
   * rotates an JPEG image lossless
   *
   * @param file
   * @param degrees
   */
  public static void rotateJPEG(File file, int degrees) {
    try {
      final String commandRot = "jpegtran -rotate " + degrees + " -trim " + file.getAbsolutePath() + " " + file.getAbsolutePath();
      final Runtime rt = Runtime.getRuntime();
      rt.exec(commandRot);

    } catch (final Exception e) {
      LOGGER.error("Error rotating image", e);
    }
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
    final int size = ImageUtils.getResolution(resolution);
    final Info info = ImageMagickUtils.getInfo(file);
    if (info.getImageWidth() > size || info.getImageHeight() > size) {
      return size;
    } else {
      return info.getImageWidth() > info.getImageHeight() ? info.getImageWidth() : info.getImageHeight();
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

  private static boolean isVideo(String extension) {
    return StorageUtils.getMimeType(extension).contains("video");
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
      final Info imageInfo = new Info(tmp.getAbsolutePath());
      final String cs = imageInfo.getProperty("Colorspace");
      if (cs != null) {
        return cs;
      }
    } catch (final Exception e) {
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
  public static int getNonBlankFrame(String path) throws IOException, URISyntaxException, InterruptedException, IM4JavaException {
    final ConvertCmd cmd = getConvert();
    int count = 0;
    final String dir = FilenameUtils.getFullPath(path);
    final String pathBase = FilenameUtils.getBaseName(path);
    File f = new File(dir + pathBase + "-" + count + ".jpg");
    while (f.exists()) {
      final IMOperation op = new IMOperation();
      op.addImage();
      op.shave(1, 1, true);
      op.fuzz(10.0, true);
      op.trim();
      op.addImage();
      final File trim = TempFileUtil.createTempFile("trim", ".jpg");
      try {
        cmd.run(op, f.getAbsolutePath(), trim.getAbsolutePath());
        final Info info = new Info(trim.getAbsolutePath());
        if (!info.getImageGeometry().contains("1x1")) {
          return count;
        }
      } catch (final Exception e) {
        LOGGER.info("Some problems with getting non blank frame!", e);
      } finally {
        final String newPath = f.getAbsolutePath().replace("-" + count, "-" + Integer.valueOf(count + 1));
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
    final String dir = FilenameUtils.getFullPath(path);
    final String pathBase = FilenameUtils.getBaseName(path);
    File f = new File(dir + pathBase + "-" + count + ".jpg");
    while (f.exists()) {
      final String newPath = f.getAbsolutePath().replace("-" + count, "-" + Integer.valueOf(count + 1));
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
    final String magickPath = getImageMagickInstallationPath();
    final ConvertCmd cmd = new ConvertCmd(false);
    cmd.setSearchPath(magickPath);
    return cmd;
  }

  /**
   * Create a {@link JpegtranCmd}
   *
   * @return
   * @throws IOException
   * @throws URISyntaxException
   */
  public static JpegtranCmd getJpegtran() throws IOException, URISyntaxException {
    final String magickPath = getImageMagickInstallationPath();
    final JpegtranCmd cmd = new JpegtranCmd();
    cmd.setSearchPath(magickPath);
    return cmd;
  }

  public static MogrifyCmd getMogrify() throws IOException, URISyntaxException {
    final String magickPath = getImageMagickInstallationPath();
    final MogrifyCmd cmd = new MogrifyCmd(false);
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
