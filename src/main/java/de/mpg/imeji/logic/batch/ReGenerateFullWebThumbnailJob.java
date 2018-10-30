package de.mpg.imeji.logic.batch;

import java.util.List;
import java.util.concurrent.Callable;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.storage.StorageController;

public class ReGenerateFullWebThumbnailJob implements Callable<Integer>{

	private static final Logger LOGGER = LogManager.getLogger(ReGenerateFullWebThumbnailJob.class);
	
	@Override
	public Integer call() throws Exception {
		
		LOGGER.info("Generating full web and thumbnail images for all items");
	    ContentService service = new ContentService();
	    SearchServiceAbstract<ContentVO>.RetrieveIterator iterator = service.iterateAll(10);
	    int count = 1;
	    long start = System.currentTimeMillis();
	    while (iterator.hasNext()) {
	      List<ContentVO> result = (List<ContentVO>) iterator.next();
	      for (ContentVO content : result) {
	        StorageController controller = new StorageController();
	        LOGGER.info("Generating full web and thumbnail images for item "+ content.getItemId() + " " + count + "/" + iterator.getSize());
	        try {
	          controller.reGenerateFullWebThumbnailImages(content.getOriginal());
	        } catch (Exception e) {
	          LOGGER.error("Error full web and thumbnail images for item file @ " + content.getOriginal(), e);
	        }
	        count++;
	      }
	    }

	    LOGGER.info("Full web and thumbnail images generated for all files in " + (System.currentTimeMillis() - start) + " ms!");
		return 1;
	}

}
