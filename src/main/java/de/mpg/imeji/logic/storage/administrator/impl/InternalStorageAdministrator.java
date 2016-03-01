/*
 * 
 * CDDL HEADER START
 * 
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 * 
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 * 
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 * 
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft f��r
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur F��rderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage.administrator.impl;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.ImejiSPARQL;
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
  private static final Logger LOGGER = Logger.getLogger(InternalStorageAdministrator.class);
  /**
   * The directory in file system of the {@link InternalStorage}
   */
  private File storageDir;

  /**
   * Constructor
   */
  public InternalStorageAdministrator(String storagePath) {
    this.storageDir = new File(storagePath);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getNumberOfFiles()
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
    return getNumberOfFiles(
        storageDir.getAbsolutePath() + StringHelper.fileSeparator + collectionId);
  }

  /**
   * Count the number of files for one path
   * 
   * @param directory
   * @return
   */
  private long getNumberOfFiles(String directory) {
    File f = new File(directory);
    return FileUtils.listFiles(f, FileFilterUtils.fileFileFilter(), TrueFileFilter.INSTANCE).size();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getSizeOfFiles()
   */
  @Override
  public long getSizeOfFiles() {
    return FileUtils.sizeOfDirectory(storageDir);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.storage.adminstrator.StorageAdministrator#getFreeSpace()
   */
  @Override
  public long getFreeSpace() {
    return storageDir.getUsableSpace();
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.mpg.imeji.logic.storage.administrator.StorageAdministrator#getAllFiles()
   */
  @Override
  public int clean() {
    int deleted = 0;
    LOGGER.info("Start cleaning...");
    for (File f : FileUtils.listFiles(storageDir, null, true)) {
      if (f.isFile()) {
        InternalStorageManager m = new InternalStorageManager();
        String url = m.transformPathToUrl(f.getPath());
        if (ImejiSPARQL.exec(JenaCustomQueries.selectItemIdOfFileUrl(url), null).size() == 0
            && ImejiSPARQL.exec(JenaCustomQueries.selectSpaceIdOfFileOrCollection(url), null)
                .size() == 0) {
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
