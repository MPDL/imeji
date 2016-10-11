package de.mpg.imeji.logic.controller.resource;

import java.io.File;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.contentanalysis.ContentAnalyse;
import de.mpg.imeji.logic.contentanalysis.ContentAnalyserFactory;
import de.mpg.imeji.logic.controller.ImejiController;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.user.util.QuotaUtil;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * Controller for {@link ContentVO}
 * 
 * @author saquet
 *
 */
public class ContentController extends ImejiController {
  private static final Logger LOGGER = Logger.getLogger(ContentController.class);
  private static final ReaderFacade READER = new ReaderFacade(Imeji.contentModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.contentModel);
  private static ExecutorService executor = Executors.newFixedThreadPool(10);
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC);

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
  public ContentVO create(File file, CollectionImeji c, User user) throws ImejiException {
    ContentVO contentVO = new ContentVO();
    contentVO = uploadFileToContentVO(file, contentVO, user, c);
    contentVO = create(contentVO);
    return contentVO;
  }

  /**
   * Create a ContentVO.
   * 
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public ContentVO create(ContentVO contentVO) throws ImejiException {
    contentVO.setId(IdentifierUtil.newURI(ContentVO.class));
    WRITER.create(Arrays.asList(contentVO), null, Imeji.adminUser);
    return contentVO;
  }


  /**
   * Read a {@link ContentVO}
   * 
   * @param contentId
   * @return
   * @throws ImejiException
   */
  public ContentVO read(String contentId) throws ImejiException {
    return (ContentVO) READER.read(contentId, Imeji.adminUser, new ContentVO());
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
    StorageController storageController = new StorageController();
    try {
      storageController.delete(contentId);
    } catch (Exception e) {
      // Delete file should not stop update process
      LOGGER.error("Error deleting file", e);
    }
    ContentVO contentVO = read(contentId);
    contentVO = uploadFileToContentVO(file, contentVO, user, c);
    contentVO = update(contentVO);
    return contentVO;
  }

  /**
   * Update a contentVO
   * 
   * @param contentVO
   * @return
   * @throws ImejiException
   */
  public ContentVO update(ContentVO contentVO) throws ImejiException {
    WRITER.update(Arrays.asList(contentVO), null, Imeji.adminUser, false);
    return contentVO;
  }

  /**
   * Delete a
   * 
   * @param contentId
   * @throws ImejiException
   */
  public void delete(String contentId) throws ImejiException {
    StorageController storageController = new StorageController();
    ContentVO contentVO = read(contentId);
    try {
      storageController.delete(contentVO.getOriginal());
      storageController.delete(contentVO.getPreview());
      storageController.delete(contentVO.getThumbnail());
    } finally {
      WRITER.delete(Arrays.asList(contentVO), Imeji.adminUser);
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
    StorageController sc = new StorageController();
    if (!SecurityUtil.staticAuth().createContent(user, c)) {
      throw new NotAllowedError(
          "User not Allowed to upload files in collection " + c.getIdString());
    }
    String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
    if (StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat)) {
      throw new UnprocessableError("upload_format_not_allowed");
    }
    QuotaUtil.checkQuota(user, file, c);
    UploadResult uploadResult = sc.upload(file.getName(), file, c.getIdString());
    contentVO.setOriginal(uploadResult.getOrginal());
    contentVO.setPreview(uploadResult.getWeb());
    contentVO.setThumbnail(uploadResult.getThumb());
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
    executor.submit(new ExtractFileContentAndUpdateTask(itemId, contentVO));
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
          StorageController storageController = new StorageController();
          File file = storageController.read(contentVO.getOriginal());
          if (file.exists()) {
            ContentAnalyse contentAnalyse = ContentAnalyserFactory.build().extractAll(file);
            contentVO.setFulltext(contentAnalyse.getFulltext());
            contentVO.setTechnicalMetadata(contentAnalyse.getTechnicalMetadata());
            update(contentVO);
            search.getIndexer().updatePartial(itemId, contentVO);
          }
        } catch (Exception e) {
          LOGGER.warn("Error extracting fulltext/metadata from file of file " + itemId
              + " with content id " + contentVO.getId().toString());
        }
      }
      return 1;
    }
  }
}
