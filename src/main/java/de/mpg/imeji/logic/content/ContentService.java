package de.mpg.imeji.logic.content;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.content.extraction.ContentExtractionResult;
import de.mpg.imeji.logic.content.extraction.ContentExtractorFactory;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.service.SearchServiceAbstract;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;

/**
 * Controller for {@link ContentVO}
 *
 * @author saquet
 *
 */
public class ContentService extends SearchServiceAbstract<ContentVO> implements Serializable {
  private static final long serialVersionUID = 6246549397756271848L;
  private static final Logger LOGGER = Logger.getLogger(ContentService.class);
  private final ContentController controller = new ContentController();

  public ContentService() {
    super(SearchObjectTypes.CONTENT);
  }

  /**
   * Create a {@link ContentVO}
   *
   * @param file
   * @param filename
   * @param c
   * @param user
   * @return
   * @throws ImejiException
   */
  public ContentVO create(Item item, File file, User user) throws ImejiException {
    ContentVO contentVO = ImejiFactory.newContent().setId(item.getId().toString())
        .setFilesize(item.getFileSize()).setMimetype(item.getFiletype()).build();
    contentVO = uploadFile(item, file, user);
    contentVO = controller.create(contentVO, Imeji.adminUser);
    analyzeFile(contentVO);
    return contentVO;
  }

  /**
   * Create a ContentVO for an external file. File will not be downloaded and thus not analyzed
   * 
   * @param item
   * @param externalFileUrl
   * @param user
   * @return
   * @throws ImejiException
   */
  public ContentVO create(Item item, String externalFileUrl, User user) throws ImejiException {
    ContentVO contentVO =
        ImejiFactory.newContent().setId(item.getId().toString()).setFilesize(item.getFileSize())
            .setMimetype(item.getFiletype()).setOriginal(externalFileUrl).build();
    return controller.create(contentVO, user);
  }

  /**
   * Read the content of an Item
   * 
   * @param item
   * @return
   */
  public ContentVO read(Item item) {
    return null;
  }

  /**
   * Read a {@link ContentVO}
   *
   * @param contentId
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> retrieveBatch(List<String> ids) throws ImejiException {
    return controller.retrieveBatch(ids, Imeji.adminUser);
  }

  /**
   * Retrieve a list of content in a lazy way
   * 
   * @param ids
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> retrieveBatchLazy(List<String> ids) throws ImejiException {
    return controller.retrieveBatchLazy(ids, Imeji.adminUser);
  }

  /**
   * Read a {@link ContentVO}
   *
   * @param contentId
   * @return
   * @throws ImejiException
   */
  public ContentVO readLazy(String contentId) throws ImejiException {
    return controller.retrieveLazy(contentId, Imeji.adminUser);
  }

  /**
   * Update the ContentVO. The file will get a new url
   *
   * @param contentId
   * @param file
   * @param filename
   * @param c
   * @param user
   * @return
   * @throws ImejiException
   */
  public ContentVO update(Item item, File file, User user) throws ImejiException {
    ContentVO contentVO = read(item);
    final StorageController storageController = new StorageController();
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
    } catch (final Exception e) {
      // Delete file should not stop update process
      LOGGER.error("Error deleting file", e);
    }
    contentVO = uploadFile(item, file, user);
    contentVO = controller.update(contentVO, Imeji.adminUser);
    analyzeFile(contentVO);
    return contentVO;
  }

  /**
   * Update a content with an external file url
   * 
   * @param item
   * @param externalFileUrl
   * @param user
   * @return
   * @throws ImejiException
   */
  public ContentVO update(Item item, String externalFileUrl, User user) throws ImejiException {
    ContentVO contentVO =
        ImejiFactory.newContent().setId(item.getId().toString()).setFilesize(item.getFileSize())
            .setMimetype(item.getFiletype()).setOriginal(externalFileUrl).build();
    return controller.update(contentVO, user);
  }

  /**
   * Update a contentVO
   *
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> updateBatch(List<ContentVO> contents) throws ImejiException {
    return controller.updateBatch(contents, Imeji.adminUser);
  }

  /**
   * Delete a
   *
   * @param contentId
   * @throws ImejiException
   */
  public void delete(Item item) throws ImejiException {
    final StorageController storageController = new StorageController();
    final ContentVO contentVO = read(item);
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
    } finally {
      controller.delete(contentVO, Imeji.adminUser);
    }
  }

  /**
   * Copy a {@link ContentVO} to another collection
   * 
   * @param contentId
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  public ContentVO copy(Item item, String collectionId) throws ImejiException {
    final StorageController storageController = new StorageController();
    ContentVO contentVO = read(item);
    UploadResult result = storageController.copy(contentVO.getOriginal(), collectionId);
    contentVO.setPreview(result.getWeb());
    contentVO.setThumbnail(result.getThumb());
    contentVO.setFull(result.getFull());
    contentVO.setOriginal(result.getFull());
    return controller.create(contentVO, Imeji.adminUser);
  }

  /**
   * Upload a File to the storage and add upload result to the contentVO
   *
   * @param file
   * @param filename
   * @param contentVO
   * @param user
   * @param c
   * @return
   * @throws ImejiException
   */
  private ContentVO uploadFile(Item item, File file, User user) throws ImejiException {
    ContentVO contentVO = new ContentVO();
    if (item.getId() == null) {
      throw new UnprocessableError(
          "Item id is null: Cannot upload a File to a non-existing Content");
    }
    final StorageController sc = new StorageController();
    final UploadResult uploadResult =
        sc.upload(file.getName(), file, ObjectHelper.getId(item.getCollection()));
    contentVO.setOriginal(uploadResult.getOrginal());
    contentVO.setPreview(uploadResult.getWeb());
    contentVO.setThumbnail(uploadResult.getThumb());
    contentVO.setFull(uploadResult.getFull());
    contentVO.setChecksum(uploadResult.getChecksum());
    contentVO.setFileSize(uploadResult.getFileSize());
    contentVO.setHeight(uploadResult.getHeight());
    contentVO.setWidth(uploadResult.getWidth());
    contentVO.setMimetype(item.getFiletype());
    return contentVO;
  }

  /**
   * Extract the content of the file (Fulltext and technical metadata) and add it to the contentVO.
   * This method is asynchronous
   *
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public void analyzeFile(ContentVO contentVO) throws ImejiException {
    Imeji.getCONTENT_EXTRACTION_EXECUTOR().submit(new ExtractFileContentAndUpdateTask(contentVO));
  }

  /**
   * Extract the content of the File of the contentVO. Return true if th extraction happened
   *
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public boolean extractContent(ContentVO contentVO) {
    try {
      final StorageController storageController = new StorageController();
      final File file = storageController.read(contentVO.getOriginal());
      if (file.exists()) {
        final ContentExtractionResult contentAnalyse =
            ContentExtractorFactory.build().extractAll(file);
        contentVO.setFulltext(contentAnalyse.getFulltext());
        contentVO.setTechnicalMetadata(contentAnalyse.getTechnicalMetadata());
        return true;
      }
    } catch (final Exception e) {
      LOGGER.error("Error extracting content ", e);
    }
    return false;
  }

  /**
   * Reindex all items
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing Content...");
    final ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.content, ElasticService.ANALYSER);
    final List<ContentVO> contents = retrieveAll();
    LOGGER.info("+++ " + contents.size() + " content to index +++");
    indexer.indexBatch(contents);
    LOGGER.info("Content reindexed!");
  }

  /**
   * Retrieve all {@link Item} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> retrieveAll() throws ImejiException {
    final List<String> ids =
        ImejiSPARQL.exec(JenaCustomQueries.selectContentAll(), Imeji.contentModel);
    return retrieveBatch(ids);
  }


  /**
   * Inner class to extract the content of a file to a contentVO asynchronously
   *
   * @author saquet
   *
   */
  private class ExtractFileContentAndUpdateTask implements Callable<Integer> {
    private final ContentVO contentVO;

    public ExtractFileContentAndUpdateTask(ContentVO contentVO) {
      this.contentVO = contentVO;
    }

    @Override
    public Integer call() throws Exception {
      if (!StringHelper.isNullOrEmptyTrim(contentVO.getId())) {
        try {
          extractContent(contentVO);
          controller.update(contentVO, Imeji.adminUser);
        } catch (final Exception e) {
          LOGGER.warn("Error extracting fulltext/metadata from file of file "
              + contentVO.getItemId() + " with content id " + contentVO.getId().toString(), e);
        }
      }
      return 1;
    }
  }

  /**
   * Transform a list of if to a list of contentVO
   *
   * @param ids
   * @return
   */
  private List<ContentVO> toObjectList(List<String> ids) {
    final List<ContentVO> list = new ArrayList<>();
    for (final String id : ids) {
      final ContentVO c = new ContentVO();
      c.setId(URI.create(id));
      list.add(c);
    }
    return list;
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public List<ContentVO> retrieve(List<String> ids, User user) throws ImejiException {
    // TODO Auto-generated method stub
    return null;
  }

}
