package de.mpg.imeji.logic.storage.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.transform.generator.ImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.MagickImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.NiceRawFileImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.PdfImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.RawFileImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleAudioImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleImageGenerator;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * Implements all process to generate the images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class ImageGeneratorManager {
  private final List<ImageGenerator> generators;
  // Only generators that will convert file to jpg with equivalent content
  private final List<ImageGenerator> fullGenerators;
  private static final Logger LOGGER = Logger.getLogger(ImageGeneratorManager.class);

  /**
   * Default constructor of {@link ImageGeneratorManager}
   */
  public ImageGeneratorManager() {
    generators = new ArrayList<ImageGenerator>();
    generators.add(new PdfImageGenerator());
    generators.add(new SimpleAudioImageGenerator());
    generators.add(new MagickImageGenerator());
    generators.add(new SimpleImageGenerator());
    generators.add(new NiceRawFileImageGenerator());
    generators.add(new RawFileImageGenerator());

    fullGenerators = new ArrayList<ImageGenerator>();
    fullGenerators.add(new MagickImageGenerator());
    fullGenerators.add(new SimpleImageGenerator());
  }

  /**
   * Generate a Thumbnail image for imeji
   *
   * @param bytes
   * @param extension
   * @return
   */
  public File generateThumbnail(File file, String extension) {
    return generate(file, extension, FileResolution.THUMBNAIL);
  }

  /**
   * Generate a Web resolution image for imeji
   *
   * @param bytes
   * @param extension
   * @return
   */
  public File generateWebResolution(File file, String extension) {
    return generate(file, extension, FileResolution.WEB);
  }

  /**
   * Generate a full resolution image. Convert the original to jpeg if possible, otherwise return
   * null
   * 
   * @param file
   * @param extension
   * @return
   */
  public File generateFullResolution(File file, String extension) {
    for (ImageGenerator generator : fullGenerators) {
      try {
        File jpeg = generator.generateJPG(file, extension);
        if (jpeg != null && jpeg.length() > 0) {
          return jpeg;
        }
      } catch (ImejiException e) {
        LOGGER.warn("Error generating image (generator: " + generator.getClass().getName(), e);
      }
    }
    return file;
  }

  /**
   * Generate an image (only jpg and gif supported here) into a smaller image according to the
   * {@link FileResolution}
   *
   * @param bytes
   * @param extension
   * @param resolution
   * @return
   */
  public File generate(File file, String extension, FileResolution resolution) {
    // if (StorageUtils.compareExtension("gif", extension)) {
    // final File gifFile = ImageMagickUtils.resizeAnimatedGif(file, resolution);
    // if (gifFile != null) {
    // return gifFile;
    // }
    // }
    return generateJpeg(file, extension, resolution);
  }

  /**
   * Generate an jpeg image in the wished size.
   *
   * @param bytes
   * @param extension
   * @param resolution
   * @return
   */
  private File generateJpeg(File file, String extension, FileResolution resolution) {
    // Make a jpg out of the file
    try {
      // File jpeg = ImageMagickUtils.resizeToJpg(file, extension, resolution);
      // if (jpeg != null) {
      // return jpeg;
      // }
      return ImageMagickUtils.resizeJpg(toJpeg(file, extension), "jpg", resolution);
      // long a = System.currentTimeMillis();
      // File jpeg = toJpeg(file, extension);
      // long b = System.currentTimeMillis();
      // File f = ImageUtils.resizeJPEG(jpeg, resolution);
      // long c = System.currentTimeMillis();
      // return ImageMagickUtils.resizeToJpg(file, extension, resolution);
      /*
       * File fMagick = ImageMagickUtils.resizeToJpg(file, extension, resolution); long d =
       * System.currentTimeMillis(); System.out.println("To jpeg: " + (b - a));
       * System.out.println("Resize: " + (c - b)); System.out.println("Resize magick: " + (d - c));
       * System.out.println("Magick result for " + resolution + ": " + fMagick.getAbsolutePath());
       */
      // return f;
    } catch (final Exception e) {
      LOGGER.error("Error generating JPEG from File: ", e);
    }
    return null;

  }

  /**
   * Uses the {@link ImageGenerator} to transform the bytes into a jpg
   *
   * @param bytes
   * @param extension
   * @return
   * @throws ImejiException
   * @throws IOException
   */
  private File toJpeg(File file, String extension) throws ImejiException {
    if (StorageUtils.compareExtension(extension, "jpg")) {
      return file;
    }
    for (final ImageGenerator imageGenerator : generators) {
      try {
        final File jpeg = imageGenerator.generateJPG(file, extension);
        if (jpeg != null && jpeg.length() > 0) {
          return jpeg;
        }
      } catch (final Exception e) {
        LOGGER.warn("Error generating image (generator: " + imageGenerator.getClass().getName(), e);
      }
    }
    throw new ImejiException("Unsupported file format (requested was " + extension + ")");
  }
}
