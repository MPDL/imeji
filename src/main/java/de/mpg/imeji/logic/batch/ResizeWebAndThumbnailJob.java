package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.storage.StorageController;

public class ResizeWebAndThumbnailJob implements Callable<Integer> {
  private static final Logger LOGGER = LogManager.getLogger(ResizeWebAndThumbnailJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Generating JPEG for all files");
    ContentService service = new ContentService();
    SearchServiceAbstract<ContentVO>.RetrieveIterator iterator = service.iterateAll(10);
    int count = 1;
    long start = System.currentTimeMillis();
    while (iterator.hasNext()) {
      List<ContentVO> result = (List<ContentVO>) iterator.next();
      for (ContentVO content : result) {
        StorageController controller = new StorageController();
        LOGGER.info("Generating jpeg for file " + count + "/" + iterator.getSize());
        try {
          controller.recalculateWebAndThumbnail(content.getOriginal());
        } catch (Exception e) {
          LOGGER.error("Error generating images of " + content.getOriginal(), e);
        }
        count++;
      }
    }

    LOGGER.info("JPEG for all files generated in " + (System.currentTimeMillis() - start) + " ms!");
    return 1;
  }
}
