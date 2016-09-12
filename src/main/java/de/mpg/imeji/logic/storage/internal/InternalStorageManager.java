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
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur F?rderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.logic.storage.internal;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.logic.storage.util.StorageUtils.getMimeType;
import static de.mpg.imeji.logic.storage.util.StorageUtils.guessExtension;
import static de.mpg.imeji.logic.storage.util.StorageUtils.replaceExtension;
import static de.mpg.imeji.logic.storage.util.StorageUtils.writeInOut;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.administrator.impl.InternalStorageAdministrator;
import de.mpg.imeji.logic.storage.transform.ImageGeneratorManager;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Item;

/**
 * Manage internal storage in file system
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class InternalStorageManager implements Serializable {
  private static final long serialVersionUID = -5768110924108700468L;
  /**
   * The directory path where files are stored
   */
  private String storagePath;
  /**
   * The URL used to access the storage (this is a dummy url, used by the internal storage to parse
   * file location)
   */
  private String storageUrl = null;
  /**
   * The {@link InternalStorageAdministrator}
   */
  private InternalStorageAdministrator administrator;
  private static final Logger LOGGER = Logger.getLogger(InternalStorageManager.class);

  /**
   * Constructor for a specific path and url
   *
   */
  public InternalStorageManager() {
    try {
      File storageDir = new File(PropertyReader.getProperty("imeji.storage.path"));
      storagePath = StringHelper.normalizePath(storageDir.getAbsolutePath());
      storageUrl = StringHelper.normalizeURI(PropertyReader.getProperty("imeji.instance.url"))
          + "file" + StringHelper.urlSeparator;
      administrator = new InternalStorageAdministrator(storagePath);
    } catch (Exception e) {
      LOGGER.error("Internal storage couldn't be initialized!!!!!", e);
    }
  }

  /**
   * Create {@link InternalStorageItem} for one {@link File}
   *
   * @param file
   * @param filename
   * @return
   */
  public InternalStorageItem createItem(File file, String filename, String collectionId) {
    try {
      InternalStorageItem item = generateInternalStorageItem(file, filename, collectionId);
      return writeItemFiles(item, file);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Replace the {@link File} stored at the passed url by the passed {@link File}
   *
   * @param file
   * @param url
   * @throws IOException
   */
  public void replaceFile(File file, String url) throws IOException {
    // Get the filextension for the thumbnail and preview generation (can be
    // jpg, gif etc.)
    // String extension = file.getName().substring(
    // file.getName().lastIndexOf(".") + 1, file.getName().length());
    String origExtension = getExtension(file.getPath());
    String guessedExtension = guessExtension(file);
    ImageGeneratorManager generatorManager = new ImageGeneratorManager();

    removeFile(url);
    if (url.contains(FileResolution.ORIGINAL.name().toLowerCase())) {
      url = replaceExtension(url, origExtension);
      copy(file, transformUrlToPath(url));
    } else if (url.contains(FileResolution.WEB.name().toLowerCase())) {
      write(generatorManager.generateWebResolution(file, guessedExtension),
          transformUrlToPath(url));
    } else if (url.contains(FileResolution.THUMBNAIL.name().toLowerCase())) {
      write(generatorManager.generateThumbnail(file, guessedExtension), transformUrlToPath(url));
    }
  }

  /**
   * Remove a {@link InternalStorageItem} from the internal storage (i.e. removes all resolutuion of
   * the {@link Item})
   *
   * @param item
   */
  public void removeItem(InternalStorageItem item) {
    if (item.getId() != null && !item.getId().trim().equals("")) {
      removeFile(transformPathToUrl(storagePath + item.getId()));
    }
  }

  /**
   * Rmove a single {@link File}
   *
   * @param url
   */
  public void removeFile(String url) {
    File f = new File(transformUrlToPath(url));
    if (f.exists()) {
      boolean deleted = FileUtils.deleteQuietly(f);
      if (!deleted) {
        throw new RuntimeException(
            "Impossible to delete the existing file. Please close all Digilib pages and try later.");
      }
    }
  }

  /**
   * Transform and url to a file system path
   *
   * @param url
   * @return
   */
  public String transformUrlToPath(String url) {
    return URI.create(url).getPath().replace(URI.create(storageUrl).getPath(), storagePath)
        .replace(StringHelper.urlSeparator, StringHelper.fileSeparator);
  }

  /**
   * Get the Storage Id according to the url
   *
   * @param url
   * @return
   */
  public String getStorageId(String url) {
    return transformUrlToPath(url).replace(storagePath, "")
        .replace(getFileName(url, StringHelper.urlSeparator), "")
        .replace(StringHelper.fileSeparator + "web" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator + "thumbnail" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator + "original" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator, StringHelper.urlSeparator);
  }

  /**
   * Transform the path of the item into a path
   *
   * @param path
   * @return
   */
  public String transformPathToUrl(String path) {
    return path.replace(storagePath, storageUrl).replace(StringHelper.fileSeparator,
        StringHelper.urlSeparator);
  }

  /**
   * Extract the filename out of a path (then use StringHelper.fileSeparator as separator), or of an
   * url(then use StringHelper.urlSeparator as separator)
   *
   * @param pathOrUrl
   * @param separator
   * @return
   */
  public String getFileName(String pathOrUrl, String separator) {
    if (pathOrUrl.endsWith(separator)) {
      pathOrUrl = pathOrUrl.substring(0, pathOrUrl.lastIndexOf(separator));
    }
    return pathOrUrl.substring(pathOrUrl.lastIndexOf(separator) + 1);
  }

  /**
   * @return the storageUrl
   */
  public String getStorageUrl() {
    return storageUrl;
  }

  /**
   * Get the storage path
   *
   * @return
   */
  public String getStoragePath() {
    return storagePath;
  }

  /**
   * @return the administrator
   */
  public StorageAdministrator getAdministrator() {
    return administrator;
  }

  /**
   * Create an {@link InternalStorageItem} for this file. Set the correct version.
   *
   * @param fileName
   * @return
   * @throws UnsupportedEncodingException
   */
  public InternalStorageItem generateInternalStorageItem(File file, String fileName,
      String collectionId) {
    String id = generateIdWithVersion(collectionId);
    InternalStorageItem item = new InternalStorageItem();
    item.setId(id);
    item.setFileName(fileName);
    item.setFileType(getMimeType(file));
    fileName =
        isNullOrEmpty(getExtension(fileName)) ? fileName + "." + guessExtension(file) : fileName;
    item.setOriginalUrl(generateUrl(id, fileName, FileResolution.ORIGINAL));
    item.setThumbnailUrl(generateUrl(id, fileName, FileResolution.THUMBNAIL));
    item.setWebUrl(generateUrl(id, fileName, FileResolution.WEB));
    return item;
  }

  /**
   * Generate the id of a file with the correct version, i.e.
   *
   * @param collectionId
   * @return
   */
  private String generateIdWithVersion(String collectionId) {
    int version = 0;
    String id = generateId(collectionId, version);
    while (exists(id)) {
      version++;
      id = generateId(collectionId, version);
    }
    return id;
  }

  /**
   * Generate the id of a file. This id is used to store the file in the filesystem
   *
   * @param collectionId
   * @return
   */
  private String generateId(String collectionId, int version) {
    String uuid = IdentifierUtil.newUniversalUniqueId();
    // split the uuid to split the number of subdirectories for each
    // collection
    return collectionId + StringHelper.urlSeparator + uuid.substring(0, 2)
        + StringHelper.urlSeparator + uuid.substring(2, 4) + StringHelper.urlSeparator
        + uuid.substring(4, 6) + StringHelper.urlSeparator + uuid.substring(6)
        + StringHelper.urlSeparator + version;
  }

  /**
   * Create the URL of the file from its filename, its id, and its resolution. Important: the
   * filename is decoded, to avoid problems by reading this url
   *
   * @param id
   * @param filename
   * @param resolution
   * @return
   * @throws UnsupportedEncodingException
   */
  public String generateUrl(String id, String filename, FileResolution resolution) {
    filename = StringHelper.normalizeFilename(filename);
    if (resolution != FileResolution.ORIGINAL) {
      String extension = getExtension(filename);
      filename = removeExtension(filename) + (extension.equals("gif") ? ".gif" : ".jpg");
    }
    return storageUrl + id + StringHelper.urlSeparator + resolution.name().toLowerCase()
        + StringHelper.urlSeparator + filename;
  }

  /**
   * Write a new file for the 3 resolution of one file
   *
   * @param item
   * @throws IOException
   * @throws Exception
   */
  private InternalStorageItem writeItemFiles(InternalStorageItem item, File file)
      throws IOException {
    ImageGeneratorManager generatorManager = new ImageGeneratorManager();
    // write web resolution file in storage
    String calculatedExtension = guessExtension(file);
    String webResolutionPath =
        write(generatorManager.generateWebResolution(file, calculatedExtension),
            transformUrlToPath(item.getWebUrl()));
    // Use Web resolution to generate Thumbnail (avoid to read the original
    // file again)
    File webResolutionFile = new File(webResolutionPath);
    write(
        generatorManager.generateThumbnail(webResolutionFile,
            FilenameUtils.getExtension(webResolutionPath)),
        transformUrlToPath(item.getThumbnailUrl()));
    // write original file in storage: simple copy the tmp file to the
    // correct path
    copy(file, transformUrlToPath(item.getOriginalUrl()));
    return item;
  }

  /**
   * Copy the file in the file system
   *
   * @param toCopy
   * @param path
   * @return
   * @throws IOException
   */
  private String copy(File toCopy, String path) throws IOException {
    File dest = new File(path);
    if (!dest.exists()) {
      dest.getParentFile().mkdirs();
      dest.createNewFile();
      FileInputStream fis = new FileInputStream(toCopy);
      FileOutputStream fos = new FileOutputStream(dest);
      writeInOut(fis, fos, true);
      return dest.getAbsolutePath();
    } else {
      throw new RuntimeException("File " + path + " already exists in internal storage!");
    }
  }

  /**
   * Write the bytes in the filesystem
   *
   * @param bytes
   * @param path
   * @return
   * @throws IOException
   */
  private String write(File srcFile, String path) throws IOException {
    File file = new File(path);
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      file.createNewFile();
      FileUtils.copyFile(srcFile, file);
      return file.getAbsolutePath();
    } else {
      throw new RuntimeException("File " + path + " already exists in internal storage!");
    }
  }

  /**
   * Return true if an id (i.e. a file) already exists, otherwise false
   *
   * @param id
   * @return
   */
  private boolean exists(String id) {
    return new File(id).exists();
  }
}
