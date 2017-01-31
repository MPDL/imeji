package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.StorageController;

public class ResizeWebAndThumbnailJob implements Callable<Integer> {


  private static final Logger LOGGER = Logger.getLogger(ResizeWebAndThumbnailJob.class);


  @Override
  public Integer call() throws Exception {
    Search search = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    List<String> result =
        search.searchString(JenaCustomQueries.selectItemAll(), null, null, 0, -1).getResults();
    for (String itemId : result) {
      String fullUrl =
          search.searchString(JenaCustomQueries.getFullUrlByItem(itemId), null, null, 0, -1)
              .getResults().get(0);
      String webUrl =
          search.searchString(JenaCustomQueries.getWebUrlByItem(itemId), null, null, 0, -1)
              .getResults().get(0);
      String thumbnailUrl =
          search.searchString(JenaCustomQueries.getThumbnailUrlByItem(itemId), null, null, 0, -1)
              .getResults().get(0);
      StorageController controller = new StorageController();
      controller.recalculateWebAndThumbnail(fullUrl, webUrl, thumbnailUrl);

    }
    LOGGER.info("All Web resolutions and thumbnails resized");
    return 1;
  }

}
