package de.mpg.imeji.logic.jobs;

import java.io.File;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.query.SPARQLQueries;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.vo.Item;

/**
 * Job which read all Items, read for each {@link Item} the size of the original
 * File, and write the items in Jena back with the file size;
 * 
 * @author saquet
 *
 */
public class RefreshFileSizeJob implements Callable<Integer> {
	private static Logger logger = Logger.getLogger(RefreshFileSizeJob.class);

	@Override
	public Integer call() throws Exception {
		logger.info("Starting refreshing the file size of all Items");
		logger.info("Deleting all sizes...");
		ImejiSPARQL.execUpdate(SPARQLQueries.deleteAllFileSize());
		logger.info("...done!");
		logger.info("Retrieving all items...");
		ItemController itemController = new ItemController();
		InternalStorageManager storageManager = new InternalStorageManager();
		Collection<Item> items = itemController.retrieveAll(Imeji.adminUser);
		logger.info("...done (found  " + items.size() + ")");
		logger.info("Reading the original file size of each item and update size");
		int count = 1;
		File f;
		String path;
		for (Item item : items) {
			logger.info(count + "/" + items.size());
			path = storageManager.transformUrlToPath(item.getFullImageUrl()
					.toString());
			f = new File(path);
			item.setFileSize(f.length());
			ImejiSPARQL.execUpdate(SPARQLQueries.insertFileSize(item.getId()
					.toString(), Long.toString(f.length())));
			count++;
		}
		logger.info("File sizes successfully refreshed!");
		return 1;
	}
}