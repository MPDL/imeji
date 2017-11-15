package de.mpg.imeji.logic.export;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.zip.ZipOutputStream;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.facade.SearchAndRetrieveFacade;
import de.mpg.imeji.logic.export.util.ExportUtil;
import de.mpg.imeji.logic.export.util.ZipUtil;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.CollectionObject;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;

public class FolderExport extends ExportAbstract {
  private static final Logger LOGGER = Logger.getLogger(FolderExport.class);
  private final CollectionImeji collection;
  private final SearchAndRetrieveFacade facade = new SearchAndRetrieveFacade();

  public FolderExport(String collectionId, User user) throws ImejiException {
    super(user);
    this.collection =
        retrieveCollection(ObjectHelper.getURI(CollectionImeji.class, collectionId).toString());
    this.name = new Date().toString().replace(" ", "_").replace(":", "-").concat(".zip");
  }

  @Override
  public void export(OutputStream out) throws ImejiException {
    final ZipOutputStream zip = new ZipOutputStream(out);
    builZip(zip, retrieveObjects(collection), collection.getName());
    ZipUtil.closeZip(zip);
  }

  private void builZip(ZipOutputStream zip, List<CollectionObject> objects, String parentPath)
      throws ImejiException {
    final List<String> collectionUris =
        objects.stream().filter(o -> o.getType() == ObjectType.COLLECTION).map(o -> o.getUri())
            .collect(Collectors.toList());
    final List<String> itemUris = objects.stream().filter(o -> o.getType() == ObjectType.ITEM)
        .map(o -> o.getUri()).collect(Collectors.toList());
    addFiles(zip, itemUris, parentPath);
    addFolders(zip, collectionUris, parentPath);
  }

  /**
   * Add the collection as Folder to the ZIP
   * 
   * @param zip
   * @param collectionUris
   * @throws ImejiException
   * @throws IOException
   */
  private void addFolders(ZipOutputStream zip, List<String> collectionUris, String parentPath)
      throws ImejiException {
    for (String uri : collectionUris) {
      CollectionImeji c = retrieveCollection(uri);
      try {
        String currentPath = ZipUtil.addFolder(zip, parentPath + "/" + c.getName(), 0);
        builZip(zip, retrieveObjects(c), currentPath);
      } catch (IOException e) {
        LOGGER.error("Error adding folder to ZIP", e);
      }

    }
  }

  /**
   * Add the Items as Files to the ZIP
   * 
   * @param zip
   * @param itemUris
   * @throws ImejiException
   */
  private void addFiles(ZipOutputStream zip, List<String> itemUris, String parentPath)
      throws ImejiException {
    final Map<String, Item> itemMap = ExportUtil.retrieveItems(itemUris, user);
    final List<ContentVO> contents = ExportUtil.retrieveContents(itemMap.values());
    for (ContentVO content : contents) {
      try {
        ZipUtil.addFile(zip, parentPath + "/" + itemMap.get(content.getItemId()).getFilename(),
            content.getOriginal(), 0);
      } catch (Exception e) {
        LOGGER.error("Error zip export", e);
      }
    }
  }


  /**
   * Retrieve all CollectionObject of the collections
   * 
   * @param collection
   * @return
   * @throws ImejiException
   */
  private List<CollectionObject> retrieveObjects(CollectionImeji collection) throws ImejiException {
    final SearchResult result = facade.search(null, collection, super.user, null, -1, 0);
    return facade.retrieveItemsAndCollections(result.getResults(), user);

  }

  /**
   * Retrieve a collection
   * 
   * @param uri
   * @return
   * @throws ImejiException
   */
  private CollectionImeji retrieveCollection(String uri) throws ImejiException {
    return new CollectionService().retrieveLazy(URI.create(uri), user);
  }

  @Override
  public String getContentType() {
    return "application/zip";
  }

}
