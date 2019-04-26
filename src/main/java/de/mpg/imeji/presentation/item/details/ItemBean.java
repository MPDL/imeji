package de.mpg.imeji.presentation.item.details;

import java.awt.Dimension;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.commons.io.FilenameUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.ReloadBeforeSaveException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.concurrency.Locks;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.core.statement.StatementService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.item.edit.single.EditItemComponent;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionObjectsController;

/**
 * Bean for a Single image
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "ItemBean")
@ViewScoped
public class ItemBean extends SuperBean {
  private static final long serialVersionUID = -4957755233785015759L;
  static final Logger LOGGER = LogManager.getLogger(ItemBean.class);
  private Item item;
  private ContentVO content;
  private String id;
  private boolean selected;
  private CollectionImeji collection;
  protected String prettyLink;
  private ItemDetailsBrowse browse = null;
  private String dateCreated;
  private String newFilename;
  private String imageUploader;
  private String discardComment;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  private int rotation;
  private int lastRotation = 0;
  String thumbnail;
  String preview;
  String fullResolution;
  String originalFile;
  private boolean edit = false;
  private ExecutorService rotationService;
  private EditItemComponent editor;

  /**
   * Construct a default {@link ItemBean}
   *
   * @
   */
  public ItemBean() {
    prettyLink = "pretty:editImage";
  }

  public void preRenderView() throws IOException {
    if (FacesContext.getCurrentInstance().getPartialViewContext().isAjaxRequest()) {
      return; // Skip ajax requests.
    }
    id = UrlHelper.getParameterValue("id");
    try {
      loadItem();
      init();
    } catch (ImejiException e) {
      FacesContext.getCurrentInstance().getExternalContext().responseSendError(404, "404_NOT_FOUND");
    }
  }

  /**
   * Initialize the {@link ItemBean}
   *
   * @return
   * @throws IOException @
   */
  public void init() {
    rotationService = Executors.newSingleThreadExecutor();
    try {
      if (item != null) {
        loadCollection(getSessionUser());
        initBrowsing();
        findItemUploader();
        selected = getSelectedItems().contains(item.getId().toString());
      }
    } catch (final Exception e) {
      LOGGER.error("Error initialitzing item page", e);
      BeanHelper.error("Error initializing page" + e.getMessage());
    }
  }

  /**
   * Load the item according to the idntifier defined in the URL
   *
   * @throws ImejiException
   *
   * @param itemBean
   */
  public void loadItem() throws ImejiException {
    item = new ItemService().retrieve(ObjectHelper.getURI(Item.class, id), getSessionUser());
    if (item == null) {
      throw new NotFoundException("LoadImage: empty");
    }
    if (item.getStatus() != Status.WITHDRAWN) {
      try {
        ContentService service = new ContentService();
        content = service.retrieveLazy(service.findContentId(item.getId().toString()));
        preview = content.getPreview();
        thumbnail = content.getThumbnail();
        fullResolution = content.getFull();
        originalFile = content.getOriginal();
      } catch (final Exception e) {
        ItemBean.LOGGER.error("No content found for " + item.getIdString(), e);
      }
    }
  }

  public void cancelEditor() {
    this.edit = false;
    try {
      loadItem();
    } catch (ImejiException imejiException) {
      ItemBean.LOGGER.error("Could not reload " + id, imejiException);
    }
  }

  public void showEditor() throws ImejiException {
    this.edit = true;
    setEditor(new EditItemComponent(item, getSessionUser(), getLocale()));
  }

  protected void initBrowsing() {
    browse = new ItemDetailsBrowse(item, "item", null, getSessionUser());
  }

  public String getOpenseadragonUrl() {
    return getNavigation().getOpenseadragonUrl() + "?id=" + content.getOriginal();
  }

  /**
   * Find the user name of the user who upload the file
   */
  private void findItemUploader() {
    imageUploader = new UserService().getCompleteName(item.getCreatedBy(), getLocale());
  }

  /**
   * Initialize the technical metadata when the "technical metadata" tab is called
   *
   * @throws ImejiException
   *
   * @
   */
  public void initViewTechnicalMetadata() throws ImejiException {
    content = new ContentService().retrieve(content.getId().toString());
  }

  /**
   * Load the collection according to the identifier defined in the URL
   */
  public void loadCollection(User user) {
    try {
      collection = new CollectionService().retrieveLazy(item.getCollection(), user);
    } catch (final Exception e) {
      BeanHelper.error(e.getMessage());
      collection = null;
      LOGGER.error("Error loading collection", e);
    }
  }

  /**
   * Return and URL encoded version of the filename
   *
   * @return
   * @throws UnsupportedEncodingException
   */
  public String getEncodedFileName() throws UnsupportedEncodingException {
    if (item == null || item.getFilename() == null) {
      return "";
    }
    return URLEncoder.encode(item.getFilename(), "UTF-8");
  }

  public void showTechnicalMetadata() {
    ContentService service = new ContentService();
    try {
      content = service.retrieve(service.findContentId(item.getId().toString()));
    } catch (ImejiException e) {
      LOGGER.error("Erro loading technical metadata", e);
    }
  }

  public void hideTechnicalMetadata() {
    // techMd = null;
  }

  public String getPageUrl() {
    return getCurrentPage().getCompleteUrl();
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public void setImage(Item item) {
    this.item = item;
  }

  public Item getImage() {
    return item;
  }

  /**
   * @param selected the selected to set
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  public boolean getSelected() {
    return selected;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getNavigationString() {
    return "pretty:item";
  }

  public void save() throws IOException {
    try {
      editor.save();
      BeanHelper.addMessage(Imeji.RESOURCE_BUNDLE.getMessage("success_editor_image", getLocale()));
      cancelEditor();
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving item metadata", e);
    } catch (ReloadBeforeSaveException reloadBeforeSave) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_metadata_edit", getLocale()) + ": " + reloadBeforeSave.getMessage());
    } catch (ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_metadata_edit", getLocale()) + ": " + e.getMessage());
      LOGGER.error("Error saving item metadata", e);
    }
  }

  /**
   * Remove the {@link Item} from the database. If the item was in the current {@link Album}, remove
   * the {@link Item} from it
   *
   * @throws ImejiException
   *
   * @
   */
  public void delete() throws ImejiException {
    new ItemService().delete(Arrays.asList(item), getSessionUser());
    new SessionObjectsController().unselectItem(item.getId().toString());
    BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename() + " "
        + Imeji.RESOURCE_BUNDLE.getMessage("success_collection_remove_from", getLocale()));
    redirectToBrowsePage();
  }

  /**
   * Discard the Item
   */
  public void withdraw() {

    try {
      new ItemService().withdraw(Arrays.asList(item), getDiscardComment(), getSessionUser());
      new SessionObjectsController().unselectItem(item.getId().toString());
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getLabel("image", getLocale()) + " " + item.getFilename() + " "
          + Imeji.RESOURCE_BUNDLE.getMessage("success_item_withdraw", getLocale()));
      this.discardComment = null;
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_withdraw_image", getLocale()));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding item:", e);
    }
    redirectToBrowsePage();
  }

  /**
   * Check if the discard comment is empty or contains only spaces
   * 
   * @return
   */
  public boolean discardCommentEmpty() {
    if (this.discardComment == null || "".equals(this.discardComment) || "".equals(this.discardComment.trim())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Listener for the discard comment
   *
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    this.discardComment = event.getNewValue().toString();
  }

  /**
   * Redirect to the browse page
   */
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getBrowseUrl());
    } catch (final IOException e) {
      LOGGER.error("Error redirect to browse page", e);
    }
  }

  /**
   * Listener of the value of the select box
   *
   * @param event
   */
  public void selectedChanged(ValueChangeEvent event) {
    final SessionObjectsController soc = new SessionObjectsController();
    if (event.getNewValue().toString().equals("true")) {
      setSelected(true);
      soc.selectItem(item.getId().toString());
    } else if (event.getNewValue().toString().equals("false")) {
      setSelected(false);
      soc.unselectItem(item.getId().toString());
    }
  }

  public List<SelectItem> getStatementMenu() throws ImejiException {
    final List<SelectItem> statementMenu = new ArrayList<SelectItem>();
    for (final Statement s : new StatementService().searchAndRetrieve(null, null, getSessionUser(), Search.GET_ALL_RESULTS,
        Search.SEARCH_FROM_START_INDEX)) {
      statementMenu.add(new SelectItem(s.getIndex(), s.getIndex()));
    }
    return statementMenu;
  }

  public boolean isLocked() {
    return Locks.isLocked(this.item.getId().toString(), getSessionUser().getEmail());
  }

  public ItemDetailsBrowse getBrowse() {
    return browse;
  }

  public void setBrowse(ItemDetailsBrowse browse) {
    this.browse = browse;
  }

  public String getDescription() {
    return item.getFilename();
  }

  /**
   * Return the {@link User} having uploaded the file for this item
   *
   * @return @
   */
  public String getImageUploader() {
    return imageUploader;
  }

  /**
   * getter
   *
   * @return
   */
  public String getItemStorageIdFilename() {
    return StringHelper.normalizeFilename(this.item.getFilename());
  }

  /**
   * True if the current file is an image
   *
   * @return
   */
  public boolean isImageFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename())).contains("image");
  }

  /**
   * True if the file is an svg
   *
   * @return
   */
  public boolean isSVGFile() {
    return "svg".equals(FilenameUtils.getExtension(content.getOriginal()));
  }

  /**
   * True if the current file is a video
   *
   * @return
   */
  public boolean isVideoFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename())).contains("video");
  }

  /**
   * True if the File is a RAW file (a file which can not be viewed in any online tool)
   *
   * @return
   */
  public boolean isRawFile() {
    return !isAudioFile() && !isVideoFile() && !isImageFile() && !isPdfFile();
  }

  /**
   * True if the current file is a pdf
   *
   * @return
   */
  public boolean isPdfFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename())).contains("application/pdf");
  }

  /**
   * True if the current file is an audio
   *
   * @return
   */
  public boolean isAudioFile() {
    return StorageUtils.getMimeType(FilenameUtils.getExtension(item.getFilename())).contains("audio");
  }

  public String getFilenameAsJpeg() {
    return FilenameUtils.removeExtension(item.getFilename()) + ".jpg";
  }

  /**
   * @return the dateCreated
   */
  public String getDateCreated() {
    return dateCreated;
  }

  /**
   * @param dateCreated the dateCreated to set
   */
  public void setDateCreated(String dateCreated) {
    this.dateCreated = dateCreated;
  }

  public String getNewFilename() {
    this.newFilename = getImage().getFilename();
    return newFilename;
  }

  public void setNewFilename(String newFilename) {
    if (!"".equals(newFilename)) {
      getImage().setFilename(newFilename);
    }
  }

  /**
   * @return the discardComment
   */
  public String getDiscardComment() {
    return discardComment;
  }

  /**
   * @param discardComment the discardComment to set
   */
  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  public List<String> getSelectedItems() {
    return selectedItems;
  }

  public void setSelectedItems(List<String> selectedItems) {
    this.selectedItems = selectedItems;
  }

  /**
   * @return the content
   */
  public ContentVO getContent() {
    return content;
  }

  /**
   * @param content the content to set
   */
  public void setContent(ContentVO content) {
    this.content = content;
  }

  public Item getItem() {
    return item;
  }

  public void setItem(Item item) {
    this.item = item;
  }

  public int getThumbnailWidth() {
    return Integer.parseInt(Imeji.PROPERTIES.getProperty("xsd.resolution.thumbnail"));
  }

  /**
   * Called when a picture is rotated by Openseadragon. If the user is authorized to rotate the
   * image, webresolution and thumbnail get rotated
   *
   * @throws Exception
   * @throws IOException
   */
  public void updateRotation() throws IOException, Exception {
    if (SecurityUtil.authorization().update(getSessionUser(), getImage())) {
      int degrees = (rotation - lastRotation + 360) % 360;
      lastRotation = rotation;
      rotationService.submit(new RotationJob(degrees));
    }
  }

  /**
   * Rotate the files to the degrees
   * 
   * @param degrees
   * @throws IOException
   * @throws Exception
   */
  public void rotate(int degrees) throws IOException, Exception {
    new StorageController().rotate(getContent().getFull(), degrees);
    long width = getContent().getWidth();
    getContent().setWidth(getContent().getHeight());
    getContent().setHeight(width);
    showTechnicalMetadata();
    setContent(new ContentService().update(getContent()));
  }

  private class RotationJob implements Callable<Integer> {
    int degrees;

    public RotationJob(int degrees) {
      this.degrees = degrees;
    }

    @Override
    public Integer call() throws Exception {
      rotate(degrees);
      return 1;
    }
  }

  public long getViewerHeight() {
    int webSize = Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
    int imgWidth = (int) getContent().getWidth();
    int imgHeight = (int) getContent().getHeight();
    return content == null || content.getWidth() == 0 ? Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth())
        : (imgHeight > imgWidth ? webSize : (content.getHeight() * webSize / content.getWidth()));
  }

  /**
   * Gets the width of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionWidth() throws IOException {
    int webSize = Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
    int imgWidth = (int) getContent().getWidth();
    int imgHeight = (int) getContent().getHeight();
    if (isViewInOpenseadragon() && imgWidth == 0 && imgHeight == 0) {
      StorageController controller = new StorageController();
      Dimension dim = controller.getImageDimension(getContent().getFull());
      getContent().setWidth((long) dim.getWidth());
      getContent().setHeight((long) dim.getHeight());

      imgWidth = (int) getContent().getWidth();
      imgHeight = (int) getContent().getHeight();
    }

    if (imgWidth < webSize && imgHeight < webSize) {
      return imgWidth;
    }
    if (imgWidth >= imgHeight) {
      return webSize;
    }
    return (int) (imgWidth * 1.0 / imgHeight * webSize);
  }

  /**
   * Gets the height of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionHeight() throws IOException {
    int webSize = Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
    int imgWidth = (int) getContent().getWidth();
    int imgHeight = (int) getContent().getHeight();
    if (isViewInOpenseadragon() && imgWidth == 0 && imgHeight == 0) {
      StorageController controller = new StorageController();
      Dimension dim = controller.getImageDimension(getContent().getFull());
      getContent().setWidth((long) dim.getWidth());
      getContent().setHeight((long) dim.getHeight());

      imgWidth = (int) getContent().getWidth();
      imgHeight = (int) getContent().getHeight();
    }
    if (imgWidth < webSize && imgHeight < webSize) {
      return imgHeight;
    }
    if (imgWidth >= imgHeight) {
      return (int) (imgHeight * 1.0 / imgWidth * webSize);
    }
    return webSize;
  }

  /**
   * Gets the max of width and height of the web resolution
   *
   * @return
   * @throws IOException
   */
  public int getWebResolutionMaxLength() throws IOException {
    return Integer.parseInt(Imeji.CONFIG.getWebResolutionWidth());
  }

  public int getFullResolutionWidth() throws IOException {
    return (int) getContent().getWidth();
  }

  public int getFullResolutionHeight() throws IOException {
    return (int) getContent().getHeight();
  }

  public int getWebResolutionTop() throws IOException {
    return (getWebResolutionMaxLength() - getWebResolutionHeight()) / 2;
  }

  public int getWebResolutionLeft() throws IOException {
    return (getWebResolutionMaxLength() - getWebResolutionWidth()) / 2;
  }

  public int getRotation() {
    return rotation;
  }

  public void setRotation(int rotation) {
    this.rotation = rotation;
  }

  // Show in osd iff it was possible to convert original to jpg
  public boolean isViewInOpenseadragon() {
    return content != null && StorageUtils.isImage(content.getOriginal());
  }

  /**
   * True if the file is a gif
   *
   * @return
   */
  public boolean isGIFFile() {
    return "gif".equals(FilenameUtils.getExtension(content.getOriginal()));
  }

  public boolean isTIFFile() {
    return "tiff".equals(FilenameUtils.getExtension(content.getOriginal()));
  }

  /**
   * @return the thumbnail
   */
  public String getThumbnail() {
    return thumbnail;
  }

  /**
   * @return the preview
   */
  public String getPreview() {
    return preview;
  }

  /**
   * @return the fullResolution
   */
  public String getFullResolution() {
    return fullResolution;
  }

  /**
   * @return the originalFile
   */
  public String getOriginalFile() {
    return originalFile;
  }

  /**
   * @return the edit
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * @param edit the edit to set
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }

  /**
   * @return the editor
   */
  public EditItemComponent getEditor() {
    return editor;
  }

  /**
   * @param editor the editor to set
   */
  public void setEditor(EditItemComponent editor) {
    this.editor = editor;
  }

}
