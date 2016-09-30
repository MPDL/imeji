/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.upload;

import static de.mpg.imeji.presentation.notification.CommonMessages.getSuccessCollectionDeleteMessage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.ocpsoft.pretty.PrettyContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.collection.CollectionBean;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.history.HistoryUtil;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Bean for the upload page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UploadBean")
@ViewScoped
public class UploadBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = -2731118794797476328L;
  private static final Logger LOGGER = Logger.getLogger(UploadBean.class);
  private CollectionImeji collection = new CollectionImeji();
  private int collectionSize = 0;
  private String id;
  private String localDirectory = null;
  private String externalUrl;
  private boolean recursive;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selected;
  @ManagedProperty(value = "#{UploadSession}")
  private UploadSession uploadSession;



  /**
   * Method checking the url parameters and triggering then the {@link UploadBean} methods
   *
   * @throws UnprocessableError
   *
   * @
   */
  @PostConstruct
  public void status() {
    readId();
    try {
      loadCollection();
      if (UrlHelper.getParameterBoolean("init")) {
        uploadSession.reset();
        getSelected().clear();
        externalUrl = null;
        localDirectory = null;
      } else if (UrlHelper.getParameterBoolean("start")) {
        upload();
      } else if (UrlHelper.getParameterBoolean("done")) {
        uploadSession.resetProperties();
      } else if ((UrlHelper.getParameterBoolean("edituploaded"))) {
        prepareBatchEdit();
      } else {
        BeanHelper.error("I can not get to the collection id ");
      }
    } catch (Exception e) {
      BeanHelper.error(e.getLocalizedMessage());
    }

  }

  /**
   * Read the id of the collection from the url
   */
  private void readId() {
    URI uri = HistoryUtil.extractURI(PrettyContext.getCurrentInstance().getRequestURL().toString());
    if (uri != null) {
      this.id = ObjectHelper.getId(uri);
    }
  }

  /**
   * Start the Upload of the items
   *
   * @
   */
  public void upload() {
    HttpServletRequest req =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    boolean isMultipart = ServletFileUpload.isMultipartContent(req);
    if (isMultipart) {
      // Parse the request
      try {
        ServletFileUpload upload = new ServletFileUpload();
        FileItemIterator iter = upload.getItemIterator(req);
        while (iter.hasNext()) {
          FileItemStream fis = iter.next();
          InputStream stream = fis.openStream();
          if (!fis.isFormField()) {
            String filename = fis.getName();
            File tmp = createTmpFile(filename);
            try {
              writeInTmpFile(tmp, stream);
              uploadFile(tmp, filename);
            } finally {
              stream.close();
              FileUtils.deleteQuietly(tmp);
            }
          }
        }
      } catch (Exception e) {
        LOGGER.error("Error upload file", e);
      }
    }
  }

  /**
   * Upload all Files from a directory
   *
   * @param path @
   */
  public String uploadFromLocalDirectory() {
    try {
      File dir = new File(localDirectory);
      int i = 0;
      if (dir.isDirectory()) {
        for (File f : FileUtils.listFiles(dir, null, recursive)) {
          uploadFile(f, f.getName());
          i++;
        }
      }
      BeanHelper.info(i + " files uploaded from " + localDirectory);
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
    }
    return "pretty:";
  }

  /**
   * Upload a file from the web
   *
   * @return @
   */
  public String uploadFromLink() {
    try {
      URL url = new URL(externalUrl);
      File tmp = createTmpFile(findFileName(url));
      try {
        StorageController externalController = new StorageController("external");
        FileOutputStream fos = new FileOutputStream(tmp);
        externalController.read(url.toString(), fos, true);
        uploadFile(tmp, findFileName(url));
        externalUrl = null;
      } catch (Exception e) {
        getfFiles().add(e.getMessage() + ": " + findFileName(url));
        LOGGER.error("Error uploading file from link: " + externalUrl, e);
      } finally {
        FileUtils.deleteQuietly(tmp);
      }
    } catch (Exception e) {
      LOGGER.error("Error uploading file from link: " + externalUrl, e);
      BeanHelper.error(e.getMessage());
    }
    HistorySession hs = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
    try {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(hs.getCurrentPage().getUrl() + "?done=1");
    } catch (IOException e) {
      LOGGER.error("Error redirecting agter upload", e);
    }
    return "";
  }

  /**
   * Find in the url the filename
   *
   * @param url
   * @return
   */
  private String findFileName(URL url) {
    String name = FilenameUtils.getName(url.getPath());
    if (isWellFormedFileName(name)) {
      return name;
    }
    name = FilenameUtils.getName(url.toString());
    if (isWellFormedFileName(name)) {
      return name;
    }
    return FilenameUtils.getName(url.getPath());
  }

  /**
   * true if the filename is well formed, i.e. has an extension
   *
   * @param filename
   * @return
   */
  private boolean isWellFormedFileName(String filename) {
    return FilenameUtils.wildcardMatch(filename, "*.???")
        || FilenameUtils.wildcardMatch(filename, "*.??")
        || FilenameUtils.wildcardMatch(filename, "*.?");
  }

  /**
   * Create a tmp file with the uploaded file
   *
   * @param fio
   * @return
   */
  private File createTmpFile(String title) {
    try {
      return TempFileUtil.createTempFile("upload", "." + FilenameUtils.getExtension(title));
    } catch (Exception e) {
      LOGGER.error("Error creating a temp file", e);
    }
    return null;
  }

  /**
   * Write an {@link InputStream} in a {@link File}
   *
   * @param tmp
   * @param fis
   * @return
   * @throws IOException
   */
  private File writeInTmpFile(File tmp, InputStream fis) throws IOException {
    FileOutputStream fos = new FileOutputStream(tmp);
    try {
      StorageUtils.writeInOut(fis, fos, true);
      return tmp;
    } catch (Exception e) {
      LOGGER.error("Error writing uploaded File in temp file", e);
      return null;
    } finally {
      fos.close();
      fis.close();
    }
  }

  /**
   * Throws an {@link Exception} if the file ca be upload. Works only if the file has an extension
   * (therefore, for file without extension, the validation will only occur when the file has been
   * stored locally)
   */
  private void validateName(File file, String title) {
    if (StorageUtils.hasExtension(title)) {
      if (isCheckNameUnique()) {
        // if the checkNameUnique is checked, check that two files with
        // the same name is not possible
        if (!((isImportImageToFile() || isUploadFileToItem()))
            && filenameExistsInCollection(title)) {
          LOGGER.error("There is already at least one item with the filename "
              + FilenameUtils.getBaseName(title));
        }
      }
      StorageController sc = new StorageController();
      String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
      if (StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat)) {
        LOGGER
            .error("Upload format not allowed: " + " (" + StorageUtils.guessExtension(file) + ")");
      }
    }
  }

  /**
   * Upload one File and create the {@link de.mpg.imeji.logic.vo.Item}
   *
   * @param bytes @
   */
  private Item uploadFile(File fileUploaded, String title) {
    try {
      String calculatedExtension = StorageUtils.guessExtension(fileUploaded);
      File file = fileUploaded;
      if (!fileUploaded.getName().endsWith(calculatedExtension)) {
        file = new File(file.getName() + calculatedExtension);
        FileUtils.moveFile(fileUploaded, file);
      }
      validateName(file, title);
      Item item = null;
      ItemBusinessController controller = new ItemBusinessController();
      if (isImportImageToFile()) {
        item = controller.updateThumbnail(findItemByFileName(title), file, getSessionUser());
      } else if (isUploadFileToItem()) {
        item = controller.updateFile(findItemByFileName(title), collection, file, title,
            getSessionUser());
      } else {
        item = controller.createWithFile(null, file, title, collection, getSessionUser());
      }
      getsFiles().add(new UploadItem(item));
      return item;
    } catch (Exception e) {
      getfFiles().add(e.getMessage() != null ? "File " + title + " not uploaded. "
          + Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), getLocale()) : "");
      LOGGER.error("Error uploading item: ", e);
      return null;
    }
  }

  /**
   * Search for an item in the current collection with the same filename. The filename must be
   * unique!
   *
   * @param filename
   * @return @
   * @throws ImejiException
   */
  private Item findItemByFileName(String filename) throws ImejiException {
    Search s = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    List<String> sr =
        s.searchString(JenaCustomQueries.selectContainerItemByFilename(collection.getId(),
            FilenameUtils.getBaseName(filename)), null, null, 0, -1).getResults();
    if (sr.size() == 0) {
      throw new RuntimeException(
          "No item found with the filename " + FilenameUtils.getBaseName(filename));
    }
    if (sr.size() > 1) {
      throw new RuntimeException("Filename " + FilenameUtils.getBaseName(filename) + " not unique ("

          + sr.size() + " found).");
    }
    return new ItemBusinessController().retrieveLazy(URI.create(sr.get(0)), getSessionUser());
  }

  /**
   * True if the filename is already used by an {@link Item} in this {@link CollectionImeji}
   *
   * @param filename
   * @return
   */
  private boolean filenameExistsInCollection(String filename) {
    Search s = SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.JENA);
    return s.searchString(JenaCustomQueries.selectContainerItemByFilename(collection.getId(),
        FilenameUtils.getBaseName(filename)), null, null, 0, -1).getNumberOfRecords() > 0;
  }

  /**
   * Load the collection
   * 
   * @throws ImejiException
   *
   * @
   *
   * @
   */
  public void loadCollection() throws ImejiException {
    if (id != null) {
      collection = new CollectionController()
          .retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, id), getSessionUser());
      isDiscaded();
      if (collection != null && getCollection().getId() != null) {
        ItemBusinessController ic = new ItemBusinessController();
        collectionSize = ic.search(collection.getId(), null, null, Imeji.adminUser, null, 0, 0)
            .getNumberOfRecords();
      }
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("error", getLocale()) + "No ID in URL");
      LOGGER.error("error loading collection");
    }
  }

  /**
   * True if the {@link CollectionImeji} is discaded
   *
   * @throws UnprocessableError
   */
  private void isDiscaded() throws UnprocessableError {
    if (collection.getStatus().equals(Status.WITHDRAWN)) {
      throw new UnprocessableError(
          Imeji.RESOURCE_BUNDLE.getMessage("error_collection_discarded_upload", getLocale()));
    }
  }

  public String createDOI() {
    try {
      String doi = UrlHelper.getParameterValue("doi");
      DoiService doiService = new DoiService();
      if (doi != null) {
        doiService.addDoiToCollection(doi, collection, getSessionUser());
      } else {
        doiService.addDoiToCollection(collection, getSessionUser());
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_doi_creation", getLocale()));
    } catch (ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation", getLocale()) + " "
          + e.getMessage());
      LOGGER.error("Error during doi creation", e);
    }
    return "";
  }


  /**
   * release the {@link CollectionImeji}
   *
   * @return
   * @throws IOException
   */
  public String release() throws IOException {
    CollectionController cc = new CollectionController();
    try {
      cc.releaseWithDefaultLicense(collection, getSessionUser());
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_release", getLocale()));
    } catch (ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_release", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error releasing collection", e);
    }
    redirect(getNavigation().getCollectionUrl() + ObjectHelper.getId(collection.getId()) + "/"
        + getNavigation().getUploadPath() + "?init=1");

    return "";

  }

  /**
   * Delete the {@link CollectionImeji}
   *
   * @return
   */
  public String delete() {
    CollectionController cc = new CollectionController();
    try {
      cc.delete(collection, getSessionUser());
      BeanHelper.info(
          getSuccessCollectionDeleteMessage(collection.getMetadata().getTitle(), getLocale()));
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_delete", getLocale()));
      LOGGER.error("Error delete collection", e);
    }
    return SessionBean.getPrettySpacePage("pretty:collections", getSelectedSpaceString());
  }

  /**
   * Discard the {@link CollectionImeji} of this {@link CollectionBean}
   *
   * @return @
   * @throws IOException
   */
  public String withdraw() throws IOException {
    CollectionController cc = new CollectionController();
    try {
      cc.withdraw(collection, getSessionUser());
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_withdraw", getLocale()));
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_withdraw", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding collection:", e);
    }
    redirect(getNavigation().getCollectionUrl() + ObjectHelper.getId(collection.getId()));

    return "";

  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public int getCollectionSize() {
    return collectionSize;
  }

  public void setCollectionSize(int collectionSize) {
    this.collectionSize = collectionSize;
  }

  public String getExternalUrl() {
    return externalUrl;
  }

  public void setExternalUrl(String externalUrl) {
    this.externalUrl = externalUrl;
  }

  public List<String> getfFiles() {
    return uploadSession.getfFiles();
  }

  public List<UploadItem> getsFiles() {
    return uploadSession.getsFiles();
  }

  public List<UploadItem> getItemsToEdit() {
    return uploadSession.getItemsToEdit();
  }

  public void resetItemsToEdit() {
    uploadSession.getItemsToEdit().clear();
  }


  private boolean isCheckNameUnique() {
    return uploadSession.isCheckNameUnique();
  }

  private boolean isImportImageToFile() {
    return uploadSession.isImportImageToFile();
  }

  private boolean isUploadFileToItem() {
    return uploadSession.isUploadFileToItem();
  }

  public String getDiscardComment() {
    return collection.getDiscardComment();
  }

  public void setDiscardComment(String comment) {
    collection.setDiscardComment(comment);
  }

  public boolean isSuccessUpload() {
    return uploadSession.getsFiles().size() > 0;
  }

  /**
   * @return the localDirectory
   */
  public String getLocalDirectory() {
    return localDirectory;
  }

  /**
   * @param localDirectory the localDirectory to set
   */
  public void setLocalDirectory(String localDirectory) {
    this.localDirectory = localDirectory;
  }

  /**
   * @return the recursive
   */
  public boolean isRecursive() {
    return recursive;
  }

  /**
   * @param recursive the recursive to set
   */
  public void setRecursive(boolean recursive) {
    this.recursive = recursive;
  }

  /**
   * Listener for the discard comment
   *
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
      collection.setDiscardComment(event.getNewValue().toString().trim());
    }
  }


  public void prepareBatchEdit() throws IOException {
    getSelected().clear();
    for (UploadItem item : getItemsToEdit()) {
      getSelected().add(item.getId());
    }
    resetItemsToEdit();
    redirect(getNavigation().getApplicationSpaceUrl() + getNavigation().getEditPath()
        + "?type=selected&c=" + getCollection().getId().toString() + "&q=");
  }

  /**
   * @return the selected
   */
  public List<String> getSelected() {
    return selected;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(List<String> selected) {
    this.selected = selected;
  }

  public UploadSession getUploadSession() {
    return uploadSession;
  }

  public void setUploadSession(UploadSession uploadSession) {
    this.uploadSession = uploadSession;
  }

}
