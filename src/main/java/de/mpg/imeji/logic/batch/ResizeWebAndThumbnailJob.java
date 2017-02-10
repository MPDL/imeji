package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.vo.ContentVO;

public class ResizeWebAndThumbnailJob implements Callable<Integer> {


  private static final Logger LOGGER = Logger.getLogger(ResizeWebAndThumbnailJob.class);


  @Override
  public Integer call() throws Exception {
    ContentService service = new ContentService();
    List<ContentVO> result = service.retrieveAll();
    for (ContentVO content : result) {
      StorageController controller = new StorageController();
      controller.recalculateWebAndThumbnail(content.getFull(), content.getPreview(),
          content.getThumbnail());
    }
    LOGGER.info("All Web resolutions and thumbnails resized.");
    return 1;
  }

}
