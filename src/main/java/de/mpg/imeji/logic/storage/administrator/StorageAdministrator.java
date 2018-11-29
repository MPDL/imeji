package de.mpg.imeji.logic.storage.administrator;

import java.io.Serializable;

import de.mpg.imeji.logic.storage.Storage;

/**
 * This interfaces defines methods to administrate a {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface StorageAdministrator extends Serializable {
  /**
   * Return the number of files of the Storage for a certain path.
   *
   * @param path
   * @return
   */
  public long getNumberOfFiles();

  /**
   * Return the size in bytes of the complete files
   *
   * @return
   */
  public long getSizeOfFiles();

  /**
   * return the size in bytes of free disk space
   *
   * @return
   */
  public long getFreeSpace();

  /**
   * Clean the {@link Storage} (remove files which are not used)
   *
   * @return the number of removed files
   */
  public int clean();
}
