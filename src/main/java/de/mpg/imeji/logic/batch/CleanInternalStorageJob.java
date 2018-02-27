package de.mpg.imeji.logic.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.storage.internal.InternalStorageItem;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;

/**
 * Clea all unused files in the internal storage
 * 
 * @author saquet
 *
 */
public class CleanInternalStorageJob implements Callable<Integer> {
  private final ItemService itemService = new ItemService();
  private final InternalStorageManager internalStorageManager = new InternalStorageManager();
  private final InternalStorage storage = new InternalStorage();
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ALL, SEARCH_IMPLEMENTATIONS.JENA);
  private final static Logger LOGGER = Logger.getLogger(CleanInternalStorageJob.class);

  @Override
  public Integer call() throws Exception {
    removeUnusedFiles();
    repairImages();
    removeEmptyDirectories();
    LOGGER.info("Internal storage cleaned.");
    return null;
  }


  /**
   * Remove all files which are not used by any content
   * 
   * @throws ImejiException
   * @throws IOException
   */
  private void removeUnusedFiles() throws ImejiException, IOException {
    LOGGER.info("Cleaning internal storage.");
    String path = internalStorageManager.getStoragePath();
    int count = 0;
    for (Iterator<File> iterator = FileUtils.iterateFiles(new File(path), null, true); iterator
        .hasNext();) {
      final File file = (File) iterator.next();
      if (!isUsed(file)) {
        LOGGER.info("Deleting unused file: " + file.getAbsolutePath());
        boolean deleted = FileUtils.deleteQuietly(file);
        LOGGER.info(deleted ? "DONE!" : "ERROR!");
        if (deleted) {
          count++;
        }
      }
    }
    LOGGER.info(count + " files deleted from storage");
  }

  private boolean isUsed(File file) throws ImejiException, IOException {
    final String url = internalStorageManager.transformPathToUrl(file.getAbsolutePath());
    return isUsedByAnItem(url) || isLogo(url) || isFileProperties(file);
  }

  /**
   * Remove all directory without any file
   * 
   * @throws IOException
   */
  private void removeEmptyDirectories() throws IOException {
    LOGGER.info("Removing empty directories from internal storage");
    String path = internalStorageManager.getStoragePath();
    Files.walk(new File(path).toPath(), FileVisitOption.values())
        .filter(p -> p.toFile().isDirectory()).map(p -> p.toFile())
        .filter(f -> FileUtils.sizeOfDirectory(f) == 0).forEach(f -> {
          try {
            if (f.exists()) {
              FileUtils.deleteQuietly(f);
            }
            Files.deleteIfExists(f.toPath());
          } catch (Exception e) {
            LOGGER.info("Error deleting directory " + f.getAbsolutePath());
          }
        });
    LOGGER.info("Directories cleaned");
  }

  /**
   * True if the url is the url of a file of an item
   * 
   * @param url
   * @return
   * @throws ImejiException
   */
  private boolean isUsedByAnItem(String url) throws ImejiException {
    try {
      Item item = itemService.retrieveLazyForFile(url, Imeji.adminUser);
      return item != null && item.getStatus() != Status.WITHDRAWN;
    } catch (NotFoundException e) {
      return false;
    }
  }

  /**
   * True if the url belongs to an logo
   * 
   * @param url
   * @return
   */
  private boolean isLogo(String url) {
    final List<String> r =
        search.searchString(JenaCustomQueries.selectCollectionByLogoUrl(url), null, null, 0, -1)
            .getResults();
    if (!r.isEmpty() && r.get(0) != null) {
      return true;
    } else {
      return false;
    }
  }


  /**
   * True if the file is a "file.properties" and is not in an empty directory (i.e. the actual file
   * has been deleted)
   * 
   * @param file
   * @return
   * @throws IOException
   */
  private boolean isFileProperties(File file) throws IOException {
    if (!file.getName().equals("file.properties")) {
      return false;
    }
    File parent = file.getParentFile();
    return Files.walk(parent.toPath(), FileVisitOption.values())
        .filter(p -> p.toFile().isDirectory() && p.compareTo(parent.toPath()) != 0)
        .map(p -> p.toFile()).filter(f -> FileUtils.sizeOfDirectory(f) > 0).findAny().isPresent();
  }

  /**
   * Repair images: check if the content have all resolution correctly stored, if not repair it by
   * uploading the original file new and updating the content
   * 
   * @throws ImejiException
   * @throws IOException
   */
  private void repairImages() throws ImejiException, IOException {
    LOGGER.info("Repairing images");
    int count = 0;
    long start = System.currentTimeMillis();
    for (ContentVO content : new ContentService().retrieveAllLazy()) {
      if (hasOriginalFile(content)) {
        try {
          if (!hasAllResolution(content)) {
            repair(content);
            count++;
          }
        } catch (Exception e) {
          LOGGER.error("Error repairing image", e);
        }
      } else {
        LOGGER.fatal("File for content " + content.getId() + " not found!!! ");
      }
    }
    LOGGER.info(count + " images repaired in " + (System.currentTimeMillis() - start) + " ms!");
    if (count > 0) {
      removeUnusedFiles();
    }
  }

  /**
   * Upload the original new and update the content with the new resolutions
   * 
   * @param content
   * @throws ImejiException
   */
  private void repair(ContentVO content) throws ImejiException {
    content = new ContentService().retrieve(content.getId().toString());
    UploadResult res = new StorageController().upload(FilenameUtils.getName(content.getOriginal()),
        storage.read(content.getOriginal()));
    content.setThumbnail(res.getThumb());
    content.setPreview(res.getWeb());
    content.setFull(res.getFull());
    content.setOriginal(res.getOrginal());

    new ContentService().update(content);
  }

  /**
   * True if the {@link ContentVO} has an original File
   * 
   * @param content
   * @return
   */
  private boolean hasOriginalFile(ContentVO content) {
    return storage.read(content.getOriginal()).exists();
  }

  /**
   * True if the {@link ContentVO} has all 3 resolution correctly stored
   * 
   * @param item
   * @return
   */
  private boolean hasAllResolution(ContentVO content) {
    final InternalStorageItem item = initInternalStorageItemFromContent(content);
    return storage.read(item.getFullUrl()).exists() && storage.read(item.getWebUrl()).exists()
        && storage.read(item.getThumbnailUrl()).exists();

  }

  /**
   * Create a {@link InternalStorageItem} from a content
   * 
   * @param content
   * @return
   */
  private InternalStorageItem initInternalStorageItemFromContent(ContentVO content) {
    final String originalUrl = content.getOriginal();
    final InternalStorageItem item = new InternalStorageItem();
    item.setOriginalUrl(originalUrl);
    item.setFullUrl(storage.getFullResolutionUrl(originalUrl));
    item.setWebUrl(storage.getWebResolutionUrl(originalUrl));
    item.setThumbnailUrl(storage.getThumbnailUrl(originalUrl));
    return item;
  }
}
