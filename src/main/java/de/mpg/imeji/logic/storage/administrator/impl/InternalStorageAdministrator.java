package de.mpg.imeji.logic.storage.administrator.impl;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * {@link StorageAdministrator} for the {@link InternalStorage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class InternalStorageAdministrator implements StorageAdministrator {
  private static final long serialVersionUID = -2854550843193929384L;
  private static final Logger LOGGER = LogManager.getLogger(InternalStorageAdministrator.class);
  /**
   * The directory in file system of the {@link InternalStorage}
   */
  private final File storageDir;

  /**
   * Constructor
   */
  public InternalStorageAdministrator(String storagePath) {
    this.storageDir = new File(storagePath);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getNumberOfFiles
   * ()
   */
  @Override
  public long getNumberOfFiles() {
    return getNumberOfFiles(storageDir.getAbsolutePath());
  }

  /**
   * Return the number of files of one collection
   *
   * @param collectionId
   * @return
   */
  public long getNumberOfFilesOfCollection(String collectionId) {
    return getNumberOfFiles(storageDir.getAbsolutePath() + StringHelper.fileSeparator + collectionId);
  }

  /**
   * Count the number of files for one path
   *
   * @param directory
   * @return
   */
  private long getNumberOfFiles(String directory) {
    final File f = new File(directory);
    return FileUtils.listFiles(f, FileFilterUtils.fileFileFilter(), TrueFileFilter.INSTANCE).size();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getSizeOfFiles()
   */
  @Override
  public long getSizeOfFiles() {
    return FileUtils.sizeOfDirectory(storageDir);
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getFreeSpace()
   */
  @Override
  public long getFreeSpace() {
    return storageDir.getUsableSpace();
  }

  /*
   * (non-Javadoc)
   *
   * @see
   * de.mpg.imeji.logic.storage.administrator.StorageAdministrator#getAllFiles()
   */
  @Override
  public int clean() {
    int deleted = 0;
    LOGGER.info("Start cleaning...");
    for (final File f : FileUtils.listFiles(storageDir, null, true)) {
      if (f.isFile()) {
        final InternalStorageManager m = new InternalStorageManager();
        final String url = m.transformPathToUrl(f.getPath());
        if (ImejiSPARQL.exec(JenaCustomQueries.selectItemIdOfFileUrl(url), null).size() == 0) {
          // file doesn't exist, remove it
          m.removeFile(url);
          deleted++;
        }
      }
    }
    LOGGER.info("...done: " + deleted + " files deleted");
    return deleted;
  }
}
