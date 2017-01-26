package de.mpg.imeji.logic.storage.transform.generator;

import java.io.File;

import de.mpg.imeji.exceptions.ImejiException;

/**
 * Interface for all Class used to generate Thumbnails and web resolution images out of the files
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface ImageGenerator {
  /**
   * Generate a jpg image in the wished format from the passed bytes according to the filename
   * extension(jpg, png, fit, etc.)<br/>
   * If the {@link ImageGenerator} can not generate the file, return null
   *
   * @param file
   * @param extension
   * @return
   */
  public File generateJPG(File file, String extension) throws ImejiException;
}
