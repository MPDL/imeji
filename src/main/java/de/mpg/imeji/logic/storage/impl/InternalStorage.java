package de.mpg.imeji.logic.storage.impl;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.internal.InternalStorageItem;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.storage.transform.ImageGeneratorManager;
import de.mpg.imeji.logic.storage.util.ImageMagickUtils;
import de.mpg.imeji.logic.storage.util.ImageUtils;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * imeji internal {@link Storage}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class InternalStorage implements Storage {
  private static final long serialVersionUID = 7865121663793602621L;
  private static final Logger LOGGER = Logger.getLogger(InternalStorage.class);
  private static final String name = "internal";
  protected InternalStorageManager manager;
  private static final int MAX_RETRY = 5;
  private static final int WAIT_BEFORE_RETRY_SEC = 2;
  public static final String PATH_TO_NOT_FOUND_IMAGE = "images/file-icon.jpg";

  /**
   * Default Constructor
   */
  public InternalStorage() {
    try {
      manager = new InternalStorageManager();
    } catch (final Exception e) {
      throw new RuntimeException("Error initialising InternalStorageManager: ", e);
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#upload(byte[])
   */
  @Override
  public UploadResult upload(String filename, File file) {
    final InternalStorageItem item = manager.createItem(file, filename);
    return new UploadResult(item.getId(), item.getOriginalUrl(), item.getWebUrl(),
        item.getThumbnailUrl(), item.getFullUrl());
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#read(java.lang.String)
   */
  @Override
  public void read(String url, OutputStream out, boolean close) {
    retryRead(url, out, close, 0);
  }

  /**
   * Read the File and retry if error
   * 
   * @param url
   * @param out
   * @param close
   * @param counter
   */
  private void retryRead(String url, OutputStream out, boolean close, int counter) {
    final String path = manager.transformUrlToPath(url);
    try {
      final FileInputStream fis = new FileInputStream(path);
      StorageUtils.writeInOut(fis, out, close);
    } catch (final Exception e) {
      if (MAX_RETRY > counter) {
        LOGGER.info("Retrying read file: " + counter);
        try {
          TimeUnit.SECONDS.sleep(WAIT_BEFORE_RETRY_SEC);
        } catch (InterruptedException e1) {
          LOGGER.error("Error waiting to retry reading file " + e.getMessage());
        }
        retryRead(url, out, close, counter + 1);
      } else {
        LOGGER.error("Maximum retry to read file reached, reading default image");
        try {
          StorageUtils.writeInOut(InternalStorage.class.getClassLoader()
              .getResource(PATH_TO_NOT_FOUND_IMAGE).openStream(), out, close);
        } catch (IOException e1) {
          LOGGER.error("Error reading default image", e1);
        }
      }
    }
  }

  @Override
  public void readPart(String url, OutputStream out, boolean close, long offset, long length)
      throws ImejiException {
    final String path = manager.transformUrlToPath(url);
    try {
      final FileInputStream fis = new FileInputStream(path);
      StorageUtils.writeInOut(fis, out, close, offset, length);
    } catch (final Exception e) {
      throw new RuntimeException("Error reading file " + path + " in internal storage: ", e);
    }
  }

  @Override
  public File read(String url) {
    final String path = manager.transformUrlToPath(url);
    File file;
    try {
      file = new File(path);
    } catch (final Exception e) {
      LOGGER.error("Error reading file " + url, e);
      throw new RuntimeException("Error reading file " + path + " in internal storage: ", e);
    }
    return file;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#delete(java.lang.String)
   */
  @Override
  public void delete(String id) {
    manager.removeItem(new InternalStorageItem(id));
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#update(java.lang.String, byte[])
   */
  @Override
  public void changeThumbnail(String url, File file) {
    try {
      manager.changeThumbnail(file, url);
    } catch (final IOException e) {
      throw new RuntimeException(
          "Error updating file " + manager.transformUrlToPath(url) + " in internal storage: ", e);
    }
  }

  @Override
  public void update(String url, File file) throws IOException {
    manager.replaceFile(url, file);

  }

  @Override
  public void rotate(String fullUrl, int degrees) throws IOException, Exception {
    String thumbnailUrl = getThumbnailUrl(fullUrl);
    String webUrl = getWebResolutionUrl(fullUrl);
    File thumbnail = read(thumbnailUrl);
    File web = read(webUrl);
    File full = read(fullUrl);

    if (ImageMagickUtils.jpegtranEnabled) {
      ImageMagickUtils.rotateJPEG(thumbnail, degrees);
      ImageMagickUtils.rotateJPEG(web, degrees);
      ImageMagickUtils.rotateJPEG(full, degrees);
    } else {
      ImageUtils.rotate(full, degrees);
      web = ImageUtils.resizeJPEG(full, FileResolution.WEB);
      thumbnail = ImageUtils.resizeJPEG(full, FileResolution.THUMBNAIL);
      update(webUrl, web);
      update(thumbnailUrl, thumbnail);
    }
  }

  /**
   * Initialize an {@link UploadResult} for a directory
   * 
   * @param dir
   * @return
   */
  private UploadResult initUploadResult(File dir) {
    InternalStorageItem item = manager.initInternalStorageItem(dir);
    String id = manager.getStorageId(item.getFullUrl());
    return new UploadResult(id, item.getOriginalUrl(), item.getWebUrl(), item.getThumbnailUrl(),
        item.getFullUrl());
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getAdminstrator()
   */
  @Override
  public StorageAdministrator getAdministrator() {
    return manager.getAdministrator();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getCollectionId(java.lang.String)
   */
  @Override
  public String getCollectionId(String url) {
    return URI.create(url).getPath().replace(URI.create(manager.getStorageUrl()).getPath(), "")
        .split("/", 2)[0];
  }


  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#readFileStringContent(java.lang.String)
   */
  @Override
  public String readFileStringContent(String url) {
    final String pathString = manager.transformUrlToPath(url);
    final Path path = Paths.get(pathString);
    String stringFromFile = "";
    try {
      stringFromFile = new String(Files.readAllBytes(path));
    } catch (final Exception e) {
      stringFromFile = "";
    }
    return stringFromFile;
  }

  @Override
  public String getStorageId(String url) {
    return manager.getStorageId(url);
  }

  private String getThumbnailUrl(String originalUrl) {
    return originalUrl.replace("/full/", "/thumbnail/");
  }

  private String getWebResolutionUrl(String originalUrl) {
    return originalUrl.replace("/full/", "/web/");
  }

  @Override
  public Dimension getImageDimension(String url) throws IOException {
    final File file = read(url);
    return ImageUtils.getImageDimension(file);
  }


  @Override
  public void recalculateWebAndThumbnail(String fullUrl, String webUrl, String thumbnailUrl)
      throws IOException, Exception {
    File full = read(fullUrl);
    String guessedExtension = StorageUtils.guessExtension(full);
    ImageGeneratorManager manager = new ImageGeneratorManager();
    File thumbnail = manager.generateThumbnail(full, guessedExtension);
    File web = manager.generateWebResolution(full, guessedExtension);
    update(webUrl, web);
    update(thumbnailUrl, thumbnail);
  }

  @Override
  public double getContentLenght(String url) {
    final File f = new File(manager.transformUrlToPath(url));
    return f.length();
  }



}
