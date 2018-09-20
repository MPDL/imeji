package de.mpg.imeji.logic.batch;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.impl.InternalStorage;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * Clea all unused files in the internal storage
 * 
 * @author saquet
 *
 */
public class CleanInternalStorageJob implements Callable<Integer> {
  private final InternalStorageManager internalStorageManager = new InternalStorageManager();
  private final InternalStorage storage = new InternalStorage();
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ALL, SEARCH_IMPLEMENTATIONS.JENA);
  private final static Logger LOGGER = LogManager.getLogger(CleanInternalStorageJob.class);
  private HashSet<String> storageIdSet;

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning internal storage.");
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
  private synchronized void removeUnusedFiles() throws ImejiException, IOException {
    storageIdSet = initSetOfStorageId();
    LOGGER.info("Remove unused files...");
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

  /**
   * Initialize a hashset with all storageIds
   * 
   * @return
   */
  private HashSet<String> initSetOfStorageId() {
    LOGGER.info("Initializing Set of storage Ids...");
    ContentService service = new ContentService();
    ContentService.RetrieveIterator iterator = service.iterateAll(20);
    HashSet<String> set = new HashSet<>(iterator.getSize());
    while (iterator.hasNext()) {
      List<ContentVO> list = (List<ContentVO>) iterator.next();
      for (ContentVO content : list) {
        set.add(internalStorageManager.getStorageId(content.getOriginal()));
      }
    }
    LOGGER.info(
        "Initializing Set of storage Ids done! (number of content found: " + set.size() + ")");
    return set;
  }

  /**
   * True if the File is used by a {@link ContentVO}
   * 
   * @param file
   * @return
   * @throws ImejiException
   * @throws IOException
   */
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
    LOGGER.info("Removing empty directories from internal storage...");
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
            // Directory not deleted...
          }
        });
    LOGGER.info("Directories cleaned!");
  }

  /**
   * True if the url is the url of a file of an item
   * 
   * @param url
   * @return
   * @throws ImejiException
   */
  private boolean isUsedByAnItem(String url) throws ImejiException {
    return storageIdSet.contains(internalStorageManager.getStorageId(url));
  }

  /**
   * True if the url belongs to an logo
   * 
   * @param url
   * @return
   */
  private boolean isLogo(String url) {
    final List<String> r = search.searchString(JenaCustomQueries.selectCollectionByLogoStorageId(
        new InternalStorageManager().getStorageId(url)), null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
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
    ContentService service = new ContentService();
    ContentService.RetrieveIterator iterator = service.iterateAll(5);
    while (iterator.hasNext()) {
      List<ContentVO> list = (List<ContentVO>) iterator.next();
      for (ContentVO content : list) {
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
          LOGGER.fatal("File for content " + content.getId() + " not found.. deleting content...");
          service.delete(content);
        }
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
    LOGGER.info("Repairing content " + content.getId());
    UploadResult res = new StorageController().upload(FilenameUtils.getName(content.getOriginal()),
        storage.read(content.getOriginal()));
    LOGGER.info("File " + res.getOrginal() + " created. Updating content:");
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
   * True if the {@link ContentVO} has all 3 resolution correctly stored. The full resolution should
   * be a jpg
   * 
   * @param item
   * @return
   */
  private boolean hasAllResolution(ContentVO content) {
    return storage.read(content.getFull()).exists() && storage.read(content.getPreview()).exists()
        && storage.read(content.getThumbnail()).exists()
        && StorageUtils.compareExtension("jpg", FilenameUtils.getExtension(content.getFull()));
  }
}
