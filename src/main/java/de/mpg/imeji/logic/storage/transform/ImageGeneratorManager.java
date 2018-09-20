package de.mpg.imeji.logic.storage.transform;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.transform.generator.CompressedFilesImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.ImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.MagickImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.TextFileIconGenerator;
import de.mpg.imeji.logic.storage.transform.generator.PdfImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.RawFileImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleAudioImageGenerator;
import de.mpg.imeji.logic.storage.transform.generator.SimpleImageGenerator;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Implements all process to generate the images
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class ImageGeneratorManager {
  
  /**
   * List of classes that are able to create a preview image of a given file.
   * Order of the list matters: For creating a preview image the list is processed 
   * top-down and the first class that "fits" the file type creates the image file.  
   */
  private final List<ImageGenerator> generators;

  private final List<ImageGenerator> fullGenerators;
  
  private static final Logger LOGGER = LogManager.getLogger(ImageGeneratorManager.class);

  /**
   * Default constructor of {@link ImageGeneratorManager}
   */
  public ImageGeneratorManager() {
    generators = new ArrayList<ImageGenerator>();
    generators.add(new PdfImageGenerator());		   		// creates a preview for pdf files
    generators.add(new SimpleAudioImageGenerator());   		// creates a preview for audio files (file with mime type "audio")
    generators.add(new MagickImageGenerator());		   		// creates a preview for image and video files using ImageMagick project software library (see http://www.imagemagick.org/script/index.php)
    generators.add(new SimpleImageGenerator());	       		// creates a preview for image files (file with mime type "image")
    generators.add(new TextFileIconGenerator());   			// creates a preview for all files types for which a fixed icon exists (icon files are stored in src/main/resources/images/icons)
    generators.add(new CompressedFilesImageGenerator());	// creates a preview for compressed file types  
    generators.add(new RawFileImageGenerator());	   		// creates a default preview image for file types that haven't been handled above	

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
   * @throws ImejiException
   */
  public File generateFullResolution(File file, String extension) throws ImejiException {
    return createJpgPreview(file, extension);
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
    return createScaledJPGPreview(file, extension, resolution);
  }

  /**
   * Generate image/icon/preview in jpeg format for a file with a given resolution.
   *
   * @param file		File for which an preview/icon/thumbnail shall be created
   * @param extension	file extension
   * @param resolution	resolution of the preview/icon/thumbnail
   * @return
   */
  private File createScaledJPGPreview(File file, String extension, FileResolution resolution) {

    try {
      File jpgPreview = file;
      boolean fileIsJPEG = StorageUtils.compareExtension(extension, "jpg");
      if(!fileIsJPEG) {
    	  jpgPreview = this.createJpgPreview(file, extension);
      }
      File resizedJpgPreview = ImageMagickUtils.resizeJpg(jpgPreview, "jpg", resolution);
      return resizedJpgPreview;      
    } 
    catch (final Exception e) {
      LOGGER.error("Error generating JPEG preview for file: ", e);
    }
    return null;

  }

  /**
   * Uses a list of {@link ImageGenerator}s to create a preview (in jpg format) 
   * for a given file
   *
   * @param file the file for which a preview is created
   * @param extension  the file extension
   * @return File: a preview image in jpg format
   * @throws ImejiException
   * @throws IOException
   */
  private File createJpgPreview(File file, String extension) throws ImejiException {
    
	 // (a) case file has jpg format
	 if (StorageUtils.compareExtension(extension, "jpg")) {
	  File copy;
	  try {
	    copy = TempFileUtil.createTempFile("ImageGeneratorCopyJpeg" + file.getName(), "jpg");
	    FileUtils.copyFile(file, copy);
	    return copy;
	  } 
	  catch (IOException e) {
	    throw new ImejiException("Unsupported file format (requested was " + extension + ")");
	  }
    }
    
	// (b) case file doesn't have jpg format
	for (final ImageGenerator imageGenerator : generators) {
      try {
        final File jpeg = imageGenerator.generateFilePreview(file, extension);
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
