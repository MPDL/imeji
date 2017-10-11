package de.mpg.imeji.logic.core.content;

import java.io.File;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;

import com.google.common.collect.ImmutableMap;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.logic.batch.messageAggregations.NotifyUsersOnFileUploadAggregation;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.content.extraction.ContentExtractionResult;
import de.mpg.imeji.logic.core.content.extraction.ContentExtractorFactory;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.messaging.Message;
import de.mpg.imeji.logic.messaging.Message.MessageType;
import de.mpg.imeji.logic.messaging.MessageService;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ContentFactory;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;

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
  private final MessageService messageService = new MessageService();

  public class ItemWithStagedFile {
    private final Item item;
    private final UploadResult uploadResult;

    public ItemWithStagedFile(Item item, UploadResult uploadResult) {
      this.item = item;
      this.uploadResult = uploadResult;
    }

    /**
     * @return the item
     */
    public Item getItem() {
      return item;
    }

    /**
     * @return the file
     */
    public UploadResult getUploadResult() {
      return uploadResult;
    }
  }

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
    ContentVO contentVO =
        uploadFile(new ContentVO(), item.getId().toString(), item.getCollection(), file, user);
    contentVO = controller.create(contentVO, Imeji.adminUser);
    analyzeFile(contentVO);
    messageService.add(new Message(MessageType.UPLOAD_FILE,
        ObjectHelper.getId(item.getCollection()), createMessageContent(item)));
    return contentVO;
  }



  /**
   * Batch create {@link ContentVO}
   * 
   * @param itemWithFileList
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> createBatch(List<ItemWithStagedFile> itemWithFileList, User user)
      throws ImejiException {
    List<ContentVO> contents = itemWithFileList.stream()
        .map(i -> toContentVO(i.getItem().getId().toString(), i.getUploadResult()))
        .collect(Collectors.toList());
    controller.createBatch(contents, user);
    contents.stream().forEach(c -> analyzeFile(c));
    itemWithFileList.stream()
        .forEach(item -> messageService.add(
            new Message(MessageType.UPLOAD_FILE, ObjectHelper.getId(item.getItem().getCollection()),
                createMessageContent(item.getItem()))));;
    return contents;
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
    ContentVO contentVO = ImejiFactory.newContent().setItemId(item.getId().toString())
        .setOriginal(externalFileUrl).build();
    messageService.add(new Message(MessageType.UPLOAD_FILE,
        ObjectHelper.getId(item.getCollection()), createMessageContent(item)));
    return controller.create(contentVO, user);
  }

  /**
   * Retrieve the content
   * 
   * @param item
   * @return
   * @throws ImejiException
   */
  public ContentVO retrieve(String contentId) throws ImejiException {
    return controller.retrieve(contentId, Imeji.adminUser);
  }

  /**
   * Retrieve the content (Lazy)
   * 
   * @param contentId
   * @return
   * @throws ImejiException
   */
  public ContentVO retrieveLazy(String contentId) throws ImejiException {
    return controller.retrieveLazy(contentId, Imeji.adminUser);
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
   * Find the content if of an Item
   * 
   * @param itemId
   * @return
   * @throws NotFoundException
   */
  public String findContentId(String itemId) {
    return controller.getContentId(URI.create(itemId)).toString();
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
    ContentVO contentVO = retrieveLazy(findContentId(item.getId().toString()));
    final StorageController storageController = new StorageController();
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
      storageController.delete(contentVO.getFull());
    } catch (final Exception e) {
      // Delete file should not stop update process
      LOGGER.error("Error deleting file", e);
    }
    contentVO = uploadFile(contentVO, item.getId().toString(), item.getCollection(), file, user);
    contentVO = controller.update(contentVO, Imeji.adminUser);
    analyzeFile(contentVO);
    messageService.add(new Message(MessageType.CHANGE_FILE,
        ObjectHelper.getId(item.getCollection()), createMessageContent(item)));
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
    ContentVO contentVO = ImejiFactory.newContent().setId(item.getId().toString())
        .setOriginal(externalFileUrl).build();
    contentVO = controller.update(contentVO, user);
    messageService.add(new Message(MessageType.CHANGE_FILE,
        ObjectHelper.getId(item.getCollection()), createMessageContent(item)));
    return contentVO;
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

  public ContentVO update(ContentVO content) throws ImejiException {
    return controller.update(content, Imeji.adminUser);
  }

  public void delete(String contentId) throws ImejiException {
    final StorageController storageController = new StorageController();
    final ContentVO contentVO = retrieveLazy(contentId);
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
      storageController.delete(contentVO.getFull());
    } finally {
      controller.delete(contentVO, Imeji.adminUser);
    }
  }

  /**
   * Move Contents to another collection
   * 
   * @param item
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  public List<ContentVO> move(List<Item> items, List<ContentVO> contents, String collectionId)
      throws ImejiException {
    StorageController storageController = new StorageController();
    List<ContentVO> newContents = new ArrayList<>();
    for (ContentVO content : contents) {
      try {
        newContents.add(new ContentFactory().init(content)
            .setFiles(storageController.move(content.getOriginal(), collectionId)).build());
      } catch (Exception e) {
        LOGGER.error("Error moving file of content " + content.getId(), e);
      }
    }
    contents = updateBatch(newContents);
    // write message in queue
    Map<String, Item> itemMap =
        items.stream().collect(Collectors.toMap(Item::getIdString, Function.identity()));
    newContents.stream()
        .forEach(c -> messageService.add(new Message(MessageType.MOVE_ITEM, collectionId,
            createMessageContent(itemMap.get(ObjectHelper.getId(URI.create(c.getItemId())))))));

    return newContents;
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
  private ContentVO uploadFile(ContentVO contentVO, String itemId, URI collection, File file,
      User user) throws ImejiException {
    final StorageController sc = new StorageController();
    final UploadResult uploadResult =
        sc.upload(file.getName(), file, ObjectHelper.getId(collection));
    return toContentVO(itemId, uploadResult);
  }

  /**
   * Transform an uploadresult to a contentvo
   * 
   * @param itemId
   * @param uploadResult
   * @return
   */
  private ContentVO toContentVO(String itemId, UploadResult uploadResult) {
    ContentVO contentVO = new ContentVO();
    contentVO.setItemId(itemId);
    contentVO.setOriginal(uploadResult.getOrginal());
    contentVO.setPreview(uploadResult.getWeb());
    contentVO.setThumbnail(uploadResult.getThumb());
    contentVO.setFull(uploadResult.getFull());
    contentVO.setChecksum(uploadResult.getChecksum());
    contentVO.setHeight(uploadResult.getHeight());
    contentVO.setWidth(uploadResult.getWidth());
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
  private void analyzeFile(ContentVO contentVO) {
    Imeji.getCONTENT_EXTRACTION_EXECUTOR().submit(new ExtractFileContentAndUpdateTask(contentVO));
  }

  /**
   * Extract the content of the File of the contentVO. Return true if th extraction happened
   *
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  private boolean extractContent(ContentVO contentVO) {
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

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    return search.search(searchQuery, sortCri, user, null, offset, size);
  }

  @Override
  public List<ContentVO> retrieve(List<String> ids, User user) throws ImejiException {
    return retrieveBatch(ids);
  }

  /**
   * Update the fulltext and the technical metadata of all items
   *
   * @throws ImejiException
   */
  public void extractFulltextAndTechnicalMetadataForAllFiles() throws ImejiException {
    final List<ContentVO> allContents = retrieveAll();
    int count = 1;
    int countPart = 1;
    List<ContentVO> contents = new ArrayList<>();
    final ContentService contentController = new ContentService();
    for (final ContentVO contentVO : allContents) {
      final boolean extracted = contentController.extractContent(contentVO);
      count++;
      if (extracted) {
        countPart++;
        contents.add(contentVO);
        LOGGER.info(count + "/" + allContents.size() + " extracted (" + contentVO.getId() + ")");
      } else {
        LOGGER
            .info(count + "/" + allContents.size() + " NOT extracted (" + contentVO.getId() + ")");
      }
      if (countPart > 100) {
        // Update after 100 extraction to avoid to high memory consumption
        LOGGER.info("Updating " + contents.size() + " content with extracted infos...");
        contentController.updateBatch(contents);
        contents = new ArrayList<>();
        countPart = 1;
        LOGGER.info("... done!");
      }
    }
    LOGGER.info("Updating " + contents.size() + " content with extracted infos...");
    contentController.updateBatch(contents);
    LOGGER.info("... done!");
  }

  /**
   * Create the Message content for uploaded / modified files
   * 
   * @param item
   * @return
   */
  private Map<String, String> createMessageContent(Item item) {
    return ImmutableMap.of(NotifyUsersOnFileUploadAggregation.COUNT, "1",
        NotifyUsersOnFileUploadAggregation.FILENAME,
        item.getFilename() != null ? item.getFilename() : "",
        NotifyUsersOnFileUploadAggregation.ITEM_ID, item.getIdString());
  }

}
