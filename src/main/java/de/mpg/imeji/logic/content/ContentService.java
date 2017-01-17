package de.mpg.imeji.logic.content;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
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
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;

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
  public ContentVO create(Item item, File file, CollectionImeji c, User user)
      throws ImejiException {
    ContentVO contentVO = new ContentVO();
    contentVO = uploadFileToContentVO(file, contentVO, user, c);
    contentVO = create(item, contentVO);
    return contentVO;
  }

  /**
   * Create a ContentVO.
   *
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public ContentVO create(Item item, ContentVO contentVO) throws ImejiException {
    if (item.getId() != null) {
      contentVO.setItemId(item.getId().toString());
    }
    return controller.create(contentVO, Imeji.adminUser);
  }


  /**
   * Read a {@link ContentVO}
   *
   * @param contentId
   * @return
   * @throws ImejiException
   */
  public ContentVO read(String contentId) throws ImejiException {
    return controller.retrieve(contentId, Imeji.adminUser);
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
  public ContentVO update(String contentId, File file, CollectionImeji c, User user)
      throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(contentId)) {
      throw new UnprocessableError("Error updating content: Id is null");
    }
    ContentVO contentVO = read(contentId);
    final StorageController storageController = new StorageController();
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
    } catch (final Exception e) {
      // Delete file should not stop update process
      LOGGER.error("Error deleting file", e);
    }
    contentVO = uploadFileToContentVO(file, contentVO, user, c);
    return controller.update(contentVO, Imeji.adminUser);
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
  public void delete(String contentId) throws ImejiException {
    final StorageController storageController = new StorageController();
    final ContentVO contentVO = read(contentId);
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
    } finally {
      controller.delete(contentVO, Imeji.adminUser);
    }
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
  private ContentVO uploadFileToContentVO(File file, ContentVO contentVO, User user,
      CollectionImeji c) throws ImejiException {
    final StorageController sc = new StorageController();
    if (!SecurityUtil.authorization().update(user, c)) {
      throw new NotAllowedError(
          "User not Allowed to upload files in collection " + c.getIdString());
    }
    final String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
    if (StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat)) {
      throw new UnprocessableError("upload_format_not_allowed");
    }
    QuotaUtil.checkQuota(user, file, c);
    final UploadResult uploadResult =
        sc.upload(file.getName(), file, c != null ? c.getIdString() : user.getId().toString());
    contentVO.setOriginal(uploadResult.getOrginal());
    contentVO.setPreview(uploadResult.getWeb());
    contentVO.setThumbnail(uploadResult.getThumb());
    contentVO.setFull(uploadResult.getFull());
    contentVO.setChecksum(uploadResult.getChecksum());
    contentVO.setFileSize(uploadResult.getFileSize());
    contentVO.setHeight(uploadResult.getHeight());
    contentVO.setWidth(uploadResult.getWidth());
    contentVO.setMimetype(StorageUtils.getMimeType(guessedNotAllowedFormat));
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
  public void extractFileContentAndUpdateContentVOAsync(String itemId, ContentVO contentVO)
      throws ImejiException {
    Imeji.getCONTENT_EXTRACTION_EXECUTOR()
        .submit(new ExtractFileContentAndUpdateTask(itemId, contentVO));
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
    private final String itemId;

    public ExtractFileContentAndUpdateTask(String itemId, ContentVO contentVO) {
      this.contentVO = contentVO;
      this.itemId = itemId;
    }

    @Override
    public Integer call() throws Exception {
      if (itemId != null && !StringHelper.isNullOrEmptyTrim(contentVO.getId())) {
        try {
          extractContent(contentVO);
          contentVO.setItemId(itemId);
          controller.update(contentVO, Imeji.adminUser);
        } catch (final Exception e) {
          LOGGER.warn("Error extracting fulltext/metadata from file of file " + itemId
              + " with content id " + contentVO.getId().toString(), e);
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
