package de.mpg.imeji.logic.export;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.storage.StorageController;

/**
 * {@link ExportAbstract} images in zip
 *
 * @author kleinfercher (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ZIPExport extends ExportAbstract {
  private static final Logger LOGGER = Logger.getLogger(ZIPExport.class);
  private final Map<String, Integer> itemsPerCollection = new HashMap<String, Integer>();
  private final List<String> itemIds;

  public ZIPExport(List<String> itemIds, User user) {
    super(user);
    this.itemIds = itemIds;
    this.name = new Date().toString().replace(" ", "_").replace(":", "-").concat(".zip");
  }

  @Override
  public void export(OutputStream out) throws ImejiException {
    exportAllImages(itemIds, out, user);
  }

  @Override
  public String getContentType() {
    return "application/zip";
  }

  /**
   * This method exports all images of the current browse page as a zip file
   *
   * @throws ImejiException
   *
   * @throws Exception
   * @throws URISyntaxException
   */
  private void exportAllImages(List<String> ids, OutputStream out, User user)
      throws ImejiException {
    final ZipOutputStream zip = new ZipOutputStream(out);
    final Map<String, Item> itemMap = retrieveItems(ids);
    final List<ContentVO> contents = retrieveContents(itemMap.values());
    createItemsPerCollection(itemMap.values());
    // Create the ZIP file
    for (ContentVO content : contents) {
      try {
        addZipEntry(zip, itemMap.get(content.getItemId()).getFilename(), content.getOriginal(), 0);
      } catch (Exception e) {
        LOGGER.error("Error zip export", e);
      }
    }
    try {
      // Complete the ZIP file
      zip.close();
    } catch (final IOException ioe) {
      LOGGER.info("Could not close the ZIP File!", ioe);
    }
  }

  /**
   * Add a {@link ZipEntry} to the {@link ZipOutputStream}
   * 
   * @param zip
   * @param filename
   * @param fileUrl
   * @throws IOException
   * @throws ImejiException
   */
  private void addZipEntry(ZipOutputStream zip, String filename, String fileUrl, int position)
      throws IOException, ImejiException {
    try {
      if (position > 0) {
        filename = FilenameUtils.getBaseName(filename).replace("_" + (position - 1), "") + "_"
            + position + "." + FilenameUtils.getExtension(filename);
      }
      zip.putNextEntry(new ZipEntry(filename));
      new StorageController().read(fileUrl, zip, false);
      zip.closeEntry();
    } catch (final ZipException ze) {
      if (ze.getMessage().contains("duplicate entry")) {
        addZipEntry(zip, filename, fileUrl, position + 1);
      } else {
        throw ze;
      }
    }
  }

  private void createItemsPerCollection(Collection<Item> items) {
    for (Item item : items) {
      if (itemsPerCollection.containsKey(item.getCollection().toString())) {
        final int newVal = itemsPerCollection.get(item.getCollection().toString()).intValue() + 1;
        itemsPerCollection.put(item.getCollection().toString(), Integer.valueOf(newVal));
      } else {
        itemsPerCollection.put(item.getCollection().toString(), new Integer(1));
      }
    }
  }

  /**
   * Retrieve the contents as a map [ContentId, ContentVO]
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  private List<ContentVO> retrieveContents(Collection<Item> items) throws ImejiException {
    List<ContentVO> l = new ArrayList<>();
    ContentService service = new ContentService();
    for (Item item : items) {
      ContentVO content = service.retrieveLazy(service.findContentId(item.getId().toString()));
      l.add(content);
    }
    return l;
  }

  /**
   * Retrieve the items as a Map [itemId,Item]
   * 
   * @param result
   * @return
   * @throws ImejiException
   */
  private Map<String, Item> retrieveItems(List<String> ids) throws ImejiException {
    final List<Item> items = (List<Item>) new ItemService().retrieveBatchLazy(ids, -1, 0, user);
    Map<String, Item> map = new HashMap<>(items.size());
    for (Item item : items) {
      map.put(item.getId().toString(), item);
    }
    return map;
  }

  @Override
  public Map<String, Integer> getExportedItemsPerCollection() {
    return itemsPerCollection;
  }


}
