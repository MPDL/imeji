package de.mpg.imeji.logic.batch;

import java.io.File;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
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
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ALL, SEARCH_IMPLEMENTATIONS.JENA);
  private final static Logger LOGGER = Logger.getLogger(CleanInternalStorageJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Cleaning internal storage.");
    String path = internalStorageManager.getStoragePath();
    for (Iterator<File> iterator = FileUtils.iterateFiles(new File(path), null, true); iterator
        .hasNext();) {
      final File file = (File) iterator.next();
      final String url = internalStorageManager.transformPathToUrl(file.getAbsolutePath());
      if (!isUsedByAnItem(url) && !isLogo(url)) {
        LOGGER.info("Deleting unused file: " + file.getAbsolutePath());
        boolean deleted = FileUtils.deleteQuietly(file);
        LOGGER.info(deleted ? "DONE!" : "ERROR!");
      }
    }
    LOGGER.info("Internal storage files cleaned.");
    LOGGER.info("Removing empty directories from internal storage");;
    Files.walk(new File(path).toPath(), FileVisitOption.values())
        .filter(p -> p.toFile().isDirectory()).map(p -> p.toFile())
        .filter(f -> FileUtils.sizeOfDirectory(f) == 0).forEach(f -> {
          try {
            Files.deleteIfExists(f.toPath());
          } catch (Exception e) {
            LOGGER.info("Error deleting directory " + f.getAbsolutePath());
          }
        });
    LOGGER.info("Internal storage cleaned.");
    return null;
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
}
