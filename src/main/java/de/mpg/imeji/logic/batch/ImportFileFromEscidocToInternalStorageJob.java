package de.mpg.imeji.logic.batch;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URI;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

/**
 * Job to import files from escidoc to the internal storage. This is used when you want to switch
 * the storage from escidoc to internal
 *
 * @author saquet
 *
 */
public class ImportFileFromEscidocToInternalStorageJob implements Callable<Integer> {

  private static final Logger LOGGER =
      Logger.getLogger(ImportFileFromEscidocToInternalStorageJob.class);
  private final User user;

  public ImportFileFromEscidocToInternalStorageJob(User user) {
    this.user = user;
  }

  @Override
  public Integer call() throws Exception {
    final StorageController internal = new StorageController("internal");
    final StorageController escidoc = new StorageController("escidoc");
    final ItemService ic = new ItemService();
    for (final Item item : ic.retrieveAll(user)) {
      File tmp = null;
      try {
        // Get escidoc url for all files
        final URI escidocUrl = item.getFullImageUrl();
        LOGGER.info("Importing file " + escidocUrl + " for item " + item.getId());
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        // Read the file in a stream
        escidoc.read(escidocUrl.toString(), out, true);
        // Upload the file in the internal storage
        if (out.toByteArray() != null) {
          tmp = TempFileUtil.createTempFile("ImportFileFromEscidocToInternalStorageJob",
              FilenameUtils.getExtension(item.getFilename()));
          FileUtils.writeByteArrayToFile(tmp, out.toByteArray());
          final UploadResult result =
              internal.upload(item.getFilename(), tmp, ObjectHelper.getId(item.getCollection()));
          FileUtils.deleteQuietly(tmp);
          item.setChecksum(result.getChecksum());
          item.setFullImageUrl(URI.create(result.getOrginal()));
          item.setWebImageUrl(URI.create(result.getWeb()));
          item.setThumbnailImageUrl(URI.create(result.getThumb()));
          item.setFiletype(item.getFiletype());
          // Update the item with the new values
          ic.update(item, user);
        } else {
          LOGGER.error("File not found: " + escidocUrl + " for item " + item.getId());
        }
      } catch (final Exception e) {
        LOGGER.error("Error importing item " + item.getId(), e);
      } finally {
        FileUtils.deleteQuietly(tmp);
      }
    }
    return 1;
  }
}
