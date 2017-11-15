package de.mpg.imeji.logic.export;

import java.io.OutputStream;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.export.util.ExportUtil;
import de.mpg.imeji.logic.export.util.ZipUtil;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;

/**
 * {@link ExportAbstract} images in zip
 *
 * @author kleinfercher (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ZIPExport extends ExportAbstract {
  private static final Logger LOGGER = Logger.getLogger(ZIPExport.class);
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
    final Map<String, Item> itemMap = ExportUtil.retrieveItems(ids, user);
    final List<ContentVO> contents = ExportUtil.retrieveContents(itemMap.values());
    createItemsPerCollection(itemMap.values());
    // Create the ZIP file
    for (ContentVO content : contents) {
      try {
        ZipUtil.addFile(zip, itemMap.get(content.getItemId()).getFilename(), content.getOriginal(),
            0);
      } catch (Exception e) {
        LOGGER.error("Error zip export", e);
      }
    }
    ZipUtil.closeZip(zip);
  }
}
