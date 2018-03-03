package de.mpg.imeji.logic.batch;

import java.awt.Dimension;
import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.storage.util.ImageUtils;

/**
 * Job which read all Items, read for each {@link Item} the size of the original File, and write the
 * items in Jena back with the file size;
 *
 * @author saquet
 *
 */
public class RefreshFileSizeJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(RefreshFileSizeJob.class);

  @Override
  public Integer call() throws ImejiException {
    LOGGER.info("Starting refreshing the file size of all Items");
    LOGGER.info("Deleting all sizes...");
    ImejiSPARQL.execUpdate(JenaCustomQueries.deleteAllFileSize());
    LOGGER.info("...done!");
    LOGGER.info("Retrieving all items...");
    final ItemService itemController = new ItemService();
    final ContentService contentService = new ContentService();
    final InternalStorageManager storageManager = new InternalStorageManager();
    final SearchServiceAbstract<Item>.RetrieveIterator iterator = itemController.iterateAll(500);
    LOGGER.info("...done (found  " + iterator.getSize() + ")");
    LOGGER.info("Reading the original file size of each item and update size");
    int count = 1;
    File f;
    String path;
    while (iterator.hasNext()) {
      List<Item> items = (List<Item>) iterator.next();
      for (final Item item : items) {
        try {
          LOGGER.info(count + "/" + items.size());
          final String contentId = contentService.findContentId(item.getId().toString());
          path = storageManager
              .transformUrlToPath(contentService.retrieveLazy(contentId).getOriginal());
          f = new File(path);
          final Dimension d = ImageUtils.getImageDimension(f);
          if (d != null && d.width > 0 && d.height > 0) {
            ImejiSPARQL
                .execUpdate(JenaCustomQueries.insertFileSizeAndDimension(item.getId().toString(),
                    Long.toString(f.length()), Long.toString(d.width), Long.toString(d.height)));
          } else {
            ImejiSPARQL.execUpdate(JenaCustomQueries.insertFileSize(item.getId().toString(),
                Long.toString(f.length())));
          }

        } catch (final Exception e) {
          LOGGER.error("Error updating file size and dimension of item " + item.getIdString()
              + " : " + e.getMessage());
        } finally {
          count++;
        }
      }
    }

    LOGGER.info("File sizes successfully refreshed!");
    return 1;
  }
}
