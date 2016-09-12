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
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage;

import static de.mpg.imeji.logic.storage.util.StorageUtils.calculateChecksum;
import static de.mpg.imeji.logic.storage.util.StorageUtils.compareExtension;
import static de.mpg.imeji.logic.storage.util.StorageUtils.guessExtension;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.net.URISyntaxException;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Controller for the {@link Storage} objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public final class StorageController implements Serializable {
  private static final long serialVersionUID = -2651970941029421673L;;
  public static final String IMEJI_STORAGE_NAME_PROPERTY = "imeji.storage.name";
  private final Storage storage;
  private final String formatWhiteList;
  private final String formatBlackList;
  private static final Logger LOGGER = Logger.getLogger(StorageController.class);

  /**
   * Create new {@link StorageController} for the {@link Storage} defined in imeji.properties
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public StorageController() {
    this(null);
  }

  /**
   * Construct a {@link StorageController} for one {@link Storage}
   *
   * @param name - The name of the storage, as defined by getName() method
   */
  public StorageController(String name) {
    try {
      if (name == null) {
        name = PropertyReader.getProperty(IMEJI_STORAGE_NAME_PROPERTY);
      }
    } catch (Exception e) {
      LOGGER.error("Error initializing StorageController", e);
    }
    storage = StorageFactory.create(name);
    formatBlackList = Imeji.CONFIG.getUploadBlackList();
    formatWhiteList = Imeji.CONFIG.getUploadWhiteList();
  }

  /**
   * Call upload method of the controlled {@link Storage}
   *
   * @param filename
   * @param file
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  public UploadResult upload(String filename, File file, String collectionId)
      throws ImejiException {
    filename = FilenameUtils.getName(filename);
    UploadResult result = storage.upload(filename, file, collectionId);
    result.setChecksum(calculateChecksum(file));
    result.setFileSize(file.length());
    // If the file is an image, read the dimension of the image
    if (StorageUtils.getMimeType(file).contains("image")) {
      Dimension d = ImageUtils.getImageDimension(file);
      if (d != null) {
        result.setHeight(d.height);
        result.setWidth(d.width);
      }
    }
    return result;
  }

  /**
   * Call read method of the controlled {@link Storage}
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public void read(String url, OutputStream out, boolean close) throws ImejiException {
    storage.read(url, out, close);
  }

  /**
   * Call delete method of the controlled {@link Storage}
   *
   * @param url
   */
  public void delete(String url) {
    storage.delete(url);
  }

  /**
   * Call update method of the controlled {@link Storage}
   *
   * @param url
   * @param bytes
   */
  public void update(String url, File file) {
    storage.update(url, file);
  }

  /**
   * Return the {@link StorageAdministrator} of the current {@link Storage}
   *
   * @return
   */
  public StorageAdministrator getAdministrator() {
    return storage.getAdministrator();
  }

  /**
   * Return the id of the {@link CollectionImeji} of this file
   *
   * @return
   */
  public String getCollectionId(String url) {
    return storage.getCollectionId(url);
  }

  /**
   * Null if the file format related to the passed extension can be uploaded, not allowed file type
   * exception otherwise
   *
   * @param file
   * @return not allowed file format extension
   */
  public String guessNotAllowedFormat(File file) {
    boolean canBeUploaded = false;
    String guessedExtension = FilenameUtils.getExtension(file.getName());
    if (!"".equals(guessedExtension)) {
      canBeUploaded = isAllowedFormat(guessedExtension);
    }
    // In Any case check the extension by Tika results
    guessedExtension = guessExtension(file);

    // file can be uploaded only if both results are true
    canBeUploaded = canBeUploaded && isAllowedFormat(guessedExtension);

    return canBeUploaded ? guessedExtension : StorageUtils.BAD_FORMAT;
  }

  /**
   * True if the file format related to the passed extension can be download
   *
   * @param extension
   * @return
   */
  private boolean isAllowedFormat(String extension) {
    // If no extension, not possible to recognized the format
    // Imeji will uprfont guess the extension for the uploaded file if it is
    // not provided
    // Thus this method is not public and cannot be used as public method
    if ("".equals(extension.trim())) {
      return false;
    }
    // check in white list, if found then allowed
    for (String s : formatWhiteList.split(",")) {
      if (compareExtension(extension, s.trim())) {
        return true;
      }
    }
    // check black list, if found then forbidden
    for (String s : formatBlackList.split(",")) {
      if (compareExtension(extension, s.trim())) {
        return false;
      }
    }
    // Not found in both list: if white list is empty, allowed
    return "".equals(formatWhiteList.trim());
  }

  /**
   * Get the {@link Storage} used by the {@link StorageController}
   *
   * @return
   */
  public Storage getStorage() {
    return storage;
  }

  /**
   * Call read method of the controlled {@link Storage}
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public String readFileStringContent(String url) {
    return storage.readFileStringContent(url);
  }

  public String getFormatBlackList() {
    return formatBlackList;
  }

  public String getFormatWhiteList() {
    return formatWhiteList;
  }

}
