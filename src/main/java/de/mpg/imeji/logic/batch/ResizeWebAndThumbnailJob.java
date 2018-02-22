package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.storage.StorageController;

public class ResizeWebAndThumbnailJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(ResizeWebAndThumbnailJob.class);

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Generating JPEG for all files");
    ContentService service = new ContentService();
    List<ContentVO> result = service.retrieveAll();
    int count = 1;
    long start = System.currentTimeMillis();
    for (ContentVO content : result) {
      StorageController controller = new StorageController();
      LOGGER.info("Generating jpeg for file " + count + "/" + result.size());
      try {
        controller.recalculateWebAndThumbnail(content.getOriginal());
      } catch (Exception e) {
        LOGGER.error("Error generating images of " + content.getOriginal(), e);
      }
      count++;
    }
    LOGGER.info("JPEG for all files generated in " + (System.currentTimeMillis() - start) + " ms!");
    return 1;
  }
}
