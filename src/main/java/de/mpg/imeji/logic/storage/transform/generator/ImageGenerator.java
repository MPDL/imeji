package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;

import org.apache.commons.io.FilenameUtils;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Interface for classes that create icons and image previews for files - for general files: create
 * icons (thumbnail size, tile size) - for image files: create image previews (a thumbnail image and
 * a web resolution images)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class ImageGenerator {

  /**
   * Generate a jpg image (icon/preview) for a given file If the {@link ImageGenerator} can not
   * generate the file, return null
   *
   * @param file the file for which an icon or a preview shall be created
   * @param fileExtension the extension of the given file
   * @return the generated preview, null if a preview could not be generated
   */
  public File generateFilePreview(File file, String fileExtension) throws ImejiException {

    if (StringHelper.isNullOrEmptyTrim(fileExtension)) {
      fileExtension = FilenameUtils.getExtension(file.getName());
    }

    if (generatorSupportsMimeType(fileExtension)) {
      File fileIcon = generatePreview(file, fileExtension);
      return fileIcon;
    }
    return null;
  }

  /**
   * return true if your {@link ImageGenerator} creates an icon or image for the given file
   * extension
   * 
   * @param fileExtension
   * @return
   */
  protected abstract boolean generatorSupportsMimeType(String fileExtension);

  /**
   * 
   * @param file the file for which a preview shall be created
   * @return the generated preview (icon or image), null if there was a problem creating the preview
   * @throws ImejiException
   */

  protected abstract File generatePreview(File file, String fileExtension) throws ImejiException;

}
