package de.mpg.imeji.logic.export;

import java.io.OutputStream;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.StorageUtils;

/**
 * Export for a single File
 * 
 * @author saquet
 *
 */
public class FileExport extends ExportAbstract {
  private static final Logger LOGGER = LogManager.getLogger(FileExport.class);
  private String fileUrl;
  private String collectionId;

  public FileExport(String itemId, User user) {
    super(user);
    try {
      Item item = retrieveItem(itemId);
      ContentVO content = retrieveContent(item);
      this.collectionId = item.getCollection().toString();
      this.size = item.getFileSize();
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
    final ContentService service = new ContentService();
    return service.retrieveLazy(service.findContentId(item.getId().toString()));
  }

  @Override
  public Map<String, Integer> getExportedItemsPerCollection() {
    Map<String, Integer> map = new HashMap<>();
    map.put(collectionId, 1);
    return map;
  }
}
