package de.mpg.imeji.logic.storage.internal;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.logic.util.StorageUtils.getMimeType;
import static de.mpg.imeji.logic.util.StorageUtils.guessExtension;
import static de.mpg.imeji.logic.util.StorageUtils.replaceExtension;
import static de.mpg.imeji.logic.util.StorageUtils.writeInOut;
import static org.apache.commons.io.FilenameUtils.getExtension;
import static org.apache.commons.io.FilenameUtils.removeExtension;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.administrator.impl.InternalStorageAdministrator;
import de.mpg.imeji.logic.storage.transform.ImageGeneratorManager;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;

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
  private static final Logger LOGGER = LogManager.getLogger(InternalStorageManager.class);

  /**
   * Constructor for a specific path and url
   *
   */
  public InternalStorageManager() {
    try {
      final File storageDir = new File(PropertyReader.getProperty("imeji.storage.path"));
      storagePath = StringHelper.normalizePath(storageDir.getAbsolutePath());
      storageUrl = StringHelper.normalizeURI(PropertyReader.getProperty("imeji.instance.url")) + "file" + StringHelper.urlSeparator;
      administrator = new InternalStorageAdministrator(storagePath);
    } catch (final Exception e) {
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
  public InternalStorageItem createItem(File file, String filename) {
    try {
      final InternalStorageItem item = generateInternalStorageItem(file, filename);
      return writeItemFiles(item, file);
    } catch (final Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void replaceFile(String url, File file) throws IOException {
    removeFile(url);
    copy(file, transformUrlToPath(url));
  }

  /**
   * Replace the {@link File} stored at the passed url by the passed {@link File}
   *
   * @param file
   * @param url
   * @throws IOException
   */
  public void changeThumbnail(File file, String url) throws IOException {
    // Get the filextension for the thumbnail and preview generation (can be
    // jpg, gif etc.)
    // String extension = file.getName().substring(
    // file.getName().lastIndexOf(".") + 1, file.getName().length());
    final String origExtension = getExtension(file.getPath());
    final String guessedExtension = guessExtension(file);
    final ImageGeneratorManager generatorManager = new ImageGeneratorManager();
    removeFile(url);
    try {
      if (url.contains(FileResolution.ORIGINAL.name().toLowerCase())) {
        url = replaceExtension(url, origExtension);
        copy(file, transformUrlToPath(url));
      } else if (url.contains(FileResolution.WEB.name().toLowerCase())) {
        move(generatorManager.generateWebResolution(file, guessedExtension), transformUrlToPath(url));
      } else if (url.contains(FileResolution.THUMBNAIL.name().toLowerCase())) {
        move(generatorManager.generateThumbnail(file, guessedExtension), transformUrlToPath(url));
      }
    } finally {
      FileUtils.deleteQuietly(file);
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
      removeFile(item.getId());

    }
  }

  /**
   * Rmove a single {@link File}
   *
   * @param url
   */
  public void removeFile(String url) {
    final File f = new File(transformUrlToPath(url));
    if (f.exists()) {
      final boolean deleted = FileUtils.deleteQuietly(f);
      if (!deleted) {
        throw new RuntimeException("Impossible to delete the existing file.");
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
    return URI.create(url).getPath().replace(URI.create(storageUrl).getPath(), storagePath).replace(StringHelper.urlSeparator,
        StringHelper.fileSeparator);
  }

  /**
   * Get the Storage Id according to the url
   *
   * @param url
   * @return
   */
  public String getStorageId(String url) {
    return transformUrlToPath(url).replace(storagePath, "").replace(getFileName(url, StringHelper.urlSeparator), "")
        .replace(StringHelper.fileSeparator + "web" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator + "thumbnail" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator + "original" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator + "full" + StringHelper.fileSeparator, "")
        .replace(StringHelper.fileSeparator, StringHelper.urlSeparator);
  }

  /**
   * Transform the path of the item into a path
   *
   * @param path
   * @return
   */
  public String transformPathToUrl(String path) {
    return path.replace(storagePath, storageUrl).replace(StringHelper.fileSeparator, StringHelper.urlSeparator);
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
  public InternalStorageItem generateInternalStorageItem(File file, String fileName) {
    final String id = generateIdWithVersion();
    final InternalStorageItem item = new InternalStorageItem();
    item.setId(id);
    item.setFileName(fileName);
    item.setFileType(getMimeType(file));
    fileName =
        isNullOrEmpty(getExtension(fileName)) || "tmp".equals(getExtension(fileName)) ? fileName + "." + guessExtension(file) : fileName;
    item.setOriginalUrl(generateUrlWithEncodedFilename(id, fileName, FileResolution.ORIGINAL));
    item.setThumbnailUrl(generateUrlWithEncodedFilename(id, fileName, FileResolution.THUMBNAIL));
    item.setWebUrl(generateUrlWithEncodedFilename(id, fileName, FileResolution.WEB));
    item.setFullUrl(generateUrlWithEncodedFilename(id, fileName, FileResolution.FULL));
    return item;
  }

  /**
   * Generate the id of a file with the correct version, i.e.
   *
   * @param collectionId
   * @return
   */
  private String generateIdWithVersion() {
    int version = 0;
    String id = generateId(version);
    while (exists(id)) {
      version++;
      id = generateId(version);
    }
    return id;
  }

  /**
   * Generate the id of a file. This id is used to store the file in the filesystem
   *
   * @return
   */
  private String generateId(int version) {
    final String uuid = IdentifierUtil.newUniversalUniqueId();
    // split the uuid to split the number of subdirectories
    return uuid.substring(0, 2) + StringHelper.urlSeparator + uuid.substring(2, 4) + StringHelper.urlSeparator + uuid.substring(4, 6)
        + StringHelper.urlSeparator + uuid.substring(6) + StringHelper.urlSeparator + version;
  }

  /**
   * Create the URL of the file from its filename, its id, and its resolution. Important: the
   * filename is encoded, to avoid problems by reading this url
   *
   * @param id
   * @param filename
   * @param resolution
   * @return
   * @throws UnsupportedEncodingException
   */
  public String generateUrlWithEncodedFilename(String id, String filename, FileResolution resolution) {
    filename = StringHelper.normalizeFilename(filename);
    return generateUrl(id, filename, resolution);
  }

  public String generateUrl(String id, String filename, FileResolution resolution) {
    // Wen always convert thumbnail and preview to jpg, so we can change the
    // extension
    if (resolution != FileResolution.ORIGINAL) {
      filename = removeExtension(filename) + ".jpg";
    }
    return storageUrl + id + StringHelper.urlSeparator + resolution.name().toLowerCase() + StringHelper.urlSeparator + filename;
  }

  /**
   * Write a new file for the 3 resolution of one file
   *
   * @param item
   * @throws IOException
   * @throws Exception
   */
  private InternalStorageItem writeItemFiles(InternalStorageItem item, File file) throws IOException {
    // write original file in storage
    File original = new File(transformUrlToPath(item.getOriginalUrl()));
    move(file, original.getAbsolutePath());
    // Create thumbnail,prieview and full
    Imeji.getINTERNAL_STORAGE_EXECUTOR().submit(new GenerateThumbnailPreviewAndFullTask(item, original));
    return item;
  }

  /**
   * Return the Directory of this file
   * 
   * @param url
   * @return
   */
  public File getDirectory(String url) {
    return new File(transformUrlToPath(url)).getParentFile().getParentFile();
  }

  /**
   * Inner class to transform and write the files in the storage asynchronously
   *
   * @author saquet
   *
   */
  private class GenerateThumbnailPreviewAndFullTask implements Callable<Integer> {
    private final InternalStorageItem item;
    private final File file;

    public GenerateThumbnailPreviewAndFullTask(InternalStorageItem item, File file) {
      this.item = item;
      this.file = file;
    }

    @Override
    public Integer call() {
      generateThumbnailPreviewAndFull(item, file);
      return 1;
    }
  }

  /**
   * Generate all images for this file and this item
   * 
   * @param item
   * @param file
   */
  private void generateThumbnailPreviewAndFull(InternalStorageItem item, File file) {
    File fullResolution = null;
    try {
      final ImageGeneratorManager generatorManager = new ImageGeneratorManager();
      // write web resolution file in storage
      final String calculatedExtension = guessExtension(file);
      fullResolution = generatorManager.generateFullResolution(file, calculatedExtension);
      fullResolution = new File(move(fullResolution, transformUrlToPath(item.getFullUrl())));
      // Generate and write Web resolution
      move(generatorManager.generateWebResolution(fullResolution, "jpg"), transformUrlToPath(item.getWebUrl()));
      // Generate and write Thumbnail resolution
      move(generatorManager.generateThumbnail(fullResolution, "jpg"), transformUrlToPath(item.getThumbnailUrl()));
    } catch (final Exception e) {
      LOGGER.error("Error transforming and writing file in internal storage ", e);
    }
  }

  /**
   * Delete existing full web thumbnail images/previews of a file and create new ones Needed after
   * changes in GUI or changes in file icons
   * 
   * @param item InternalStorageItem of the file
   * @param file file for which new full web thumbnail images/previews will be generated
   */
  public void regenerateThumbnailPreviewAndFull(InternalStorageItem item, File file) {

    try {
      // (1) delete existing full web thumbnail previews
      removeFile(item.getFullUrl());
      removeFile(item.getWebUrl());
      removeFile(item.getThumbnailUrl());

      // (2) create new full web thumbnail previews
      generateThumbnailPreviewAndFull(item, file);

    } catch (final Exception e) {
      LOGGER.error("Error while regenerating full web thumbnail previews for item " + item.getId() + " ", e);
    }

  }

  /**
   * Generate the Thumbnails and the preview resolution
   * 
   * @param item
   * @param file
   */
  public void recalculateThumbnailAndPreview(InternalStorageItem item) {
    try {
      final ImageGeneratorManager generatorManager = new ImageGeneratorManager();
      File fullResolution = new File(transformUrlToPath(item.getFullUrl()));
      if (fullResolution.exists()) {
        // Generate and write web resolution from full resolution
        removeFile(item.getWebUrl());
        move(generatorManager.generateWebResolution(fullResolution, "jpg"), transformUrlToPath(item.getWebUrl()));
        // Generate and write thumbnail resolution from full resolution
        removeFile(item.getThumbnailUrl());
        move(generatorManager.generateThumbnail(fullResolution, "jpg"), transformUrlToPath(item.getThumbnailUrl()));
      }
    } catch (final Exception e) {
      LOGGER.error("Error transforming and writing file in internal storage ", e);
    }
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
    final File dest = new File(path);
    if (!dest.exists()) {
      dest.getParentFile().mkdirs();
      dest.createNewFile();
      final FileInputStream fis = new FileInputStream(toCopy);
      final FileOutputStream fos = new FileOutputStream(dest);
      writeInOut(fis, fos, true);
      return dest.getAbsolutePath();
    } else {
      throw new RuntimeException("File " + path + " already exists in internal storage!");
    }
  }

  /**
   * Move the SrcFile to the path
   * 
   * @param srcFile
   * @param path
   * @return
   * @throws IOException
   */
  public String move(File srcFile, String path) throws IOException {
    final File file = new File(path);
    if (!file.exists()) {
      file.getParentFile().mkdirs();
      FileUtils.moveFile(srcFile, file);
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
