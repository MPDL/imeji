package de.mpg.imeji.logic.export;

import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * Export for a single File
 * 
 * @author saquet
 *
 */
public class FileExport extends ExportAbstract {
  private static final Logger LOGGER = Logger.getLogger(FileExport.class);
  private String fileUrl;
  private String collectionId;

  public FileExport(String itemId, User user) {
    super(user);
    try {
      Item item = retrieveItem(itemId);
      ContentVO content = retrieveContent(item);
      this.collectionId = item.getCollection().toString();
      this.size = content.getFileSize();
      this.name = item.getFilename();
      this.fileUrl = content.getOriginal();
    } catch (Exception e) {
      LOGGER.error("Error initializing File Export", e);
    }
  }

  @Override
  public void export(OutputStream out) throws ImejiException {
    new StorageController().read(fileUrl, out, false);
  }

  @Override
  public String getContentType() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(fileUrl));
  }

  /**
   * retrieve the Item
   * 
   * @param itemId
   * @return
   * @throws ImejiException
   */
  private Item retrieveItem(String itemId) throws ImejiException {
    return new ItemService().retrieveLazy(URI.create(itemId), user);
  }

  /**
   * Retrive the content of the item
   * 
   * @param item
   * @return
   * @throws ImejiException
   */
  private ContentVO retrieveContent(Item item) throws ImejiException {
    return new ContentService().readLazy(item.getContentId());
  }

  @Override
  public Map<String, Integer> getExportedItemsPerCollection() {
    Map<String, Integer> map = new HashMap<>();
    map.put(collectionId, 1);
    return map;
  }
}
