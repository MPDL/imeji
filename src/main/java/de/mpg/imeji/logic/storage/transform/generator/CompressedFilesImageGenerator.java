package de.mpg.imeji.logic.storage.transform.generator;

import java.awt.Color;
import java.awt.Point;
import java.io.File;
import java.awt.Font;
import java.io.IOException;
import java.net.URISyntaxException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * Generates an icon/preview image for compressed/archive files types i.e. zip, tar, gz, 7z, .. Icon
 * consists of - a background image - on top of the background image we write the file extension
 * (zip, tar, etc)
 * 
 * @author breddin
 *
 */
public class CompressedFilesImageGenerator extends AbstractWritableImageGenerator {

  private static final Logger LOGGER = LogManager.getLogger(CompressedFilesImageGenerator.class);

  /**
   * Collection of mime types of archives and compressed files/archives that are supported by this
   * {@link ImageGenerator}
   */
  private static final String[] archivedMimeTypes = {"application/x-tar", "application/x-shar"};
  private static final String[] compressedMimeTypes = {"application/x-compress", "application/gzip", "application/x-lzip",
      "application/x-xz", "application/x-lzma", "application/x-snappy-framed", "application/x-lzop", "application/x-bzip2"};

  private static final String[] archivedCompressedMimeTypes =
      {"application/zip", "application/x-gtar", "application/x-ustar", "application/x-7z-compressed", "application/x-rar-compressed",
          "application/vnd.android.package-archive", "application/x-zoo", "application/x-ace-compressed", "application/x-cfs-compressed",
          "application/x-gca-compressed", "application/x-stuffit", "application/x-stuffitx"};

  /**
   * Constructor
   */
  public CompressedFilesImageGenerator() {
    super("compressed_file_icon", new Point(430, 570), new Font("Arial", Font.PLAIN, 130), Color.WHITE, 3);
  }

  @Override
  protected boolean generatorSupportsMimeType(String fileExtension) {
    String fileMimeType = StorageUtils.getMimeType(fileExtension);

    // archive?
    for (int i = 0; i < archivedMimeTypes.length; i++) {
      if (fileMimeType.equalsIgnoreCase(archivedMimeTypes[i])) {
        return true;
      }
    }

    // compressed?
    for (int i = 0; i < compressedMimeTypes.length; i++) {
      if (fileMimeType.equalsIgnoreCase(compressedMimeTypes[i])) {
        return true;
      }
    }

    // compressed archive?
    for (int i = 0; i < archivedCompressedMimeTypes.length; i++) {
      if (fileMimeType.equalsIgnoreCase(archivedCompressedMimeTypes[i])) {
        return true;
      }
    }
    return false;
  }

  /**
   * Function creates a preview image for compressed/archive files (i.e. zip, tar, 7z, z, rar)
   */
  @Override
  protected File generatePreview(File file, String fileExtension) throws ImejiException {
    try {
      File generatedImage = super.generateAndWriteOnImage(file, fileExtension);
      return generatedImage;
    } catch (URISyntaxException | IOException exception) {
      LOGGER.info("Could not create preview for " + file.getAbsolutePath() + " " + exception.getMessage());
    }

    return null;
  }

}
