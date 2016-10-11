package de.mpg.imeji.presentation.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.AjaxBehaviorEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.TypeNotAllowedException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.business.MetadataProfileBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.component.LicenseEditor;
import de.mpg.imeji.presentation.metadata.MetadataSetWrapper;
import de.mpg.imeji.presentation.metadata.MetadataWrapper;
import de.mpg.imeji.presentation.metadata.SingleEditorWrapper;
import de.mpg.imeji.presentation.metadata.extractors.TikaExtractor;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.user.UserBean;

@ManagedBean(name = "SingleUploadBean")
@ViewScoped
public class SingleUploadBean extends SuperBean implements Serializable {
  private static final long serialVersionUID = -2731118794797476328L;
  private static final Logger LOGGER = Logger.getLogger(SingleUploadBean.class);
  private List<SelectItem> collectionItems = new ArrayList<SelectItem>();
  private String selectedCollectionItem;
  private MetadataLabels metadataLabels;
  @ManagedProperty("#{SingleUploadSession}")
  private SingleUploadSession sus;
  @ManagedProperty("#{SessionBean.hasUploadRights}")
  private boolean hasUploadRights = false;
  @ManagedProperty("#{SessionBean.selectedSpaceString}")
  private String selectedSpaceString;
  private LicenseEditor licenseEditor;

  private IngestImage ingestImage;

  public SingleUploadBean() {
    // constructs...
  }

  @PostConstruct
  public void init() {
    if (getSessionUser() != null && hasUploadRights) {
      try {
        if (UrlHelper.getParameterBoolean("init")) {
          sus.reset();
        } else if (UrlHelper.getParameterBoolean("start")) {
          upload();
        } else if (UrlHelper.getParameterBoolean("done") && !UrlHelper.hasParameter("h")) {
          loadCollections();
          prepareEditor();
        }
      } catch (Exception e) {
        BeanHelper.error(e.getLocalizedMessage());
        LOGGER.error("Error initialization single upload", e);
      }
    } else if (getSessionUser() != null) {
      BeanHelper.cleanMessages();
      BeanHelper.info("You have no right to create collections, thus you can not upload items!");
      try {
        redirect(getNavigation().getHomeUrl());
      } catch (IOException e) {
        LOGGER.error("Error redirecting", e);
      }
    }

  }

  public String save() {
    try {
      Item item = ImejiFactory.newItem(getCollection());
      SingleEditorWrapper edit =
          new SingleEditorWrapper(item, sus.getProfile(), getSessionUser(), getLocale());
      MetadataSetWrapper newSet = getMdSetBean();
      edit.getEditor().getItems().get(0).setMds(newSet);
      edit.getEditor().validateAndFormatItemsForSaving();
      if (licenseEditor != null) {
        item.getLicenses().add(licenseEditor.getLicense());
      }
      uploadFileToItem(item, getCollection(), getIngestImage().getFile(),
          getIngestImage().getName());
      sus.uploaded();
      BeanHelper.cleanMessages();
      reloadItemPage(item.getIdString(), ObjectHelper.getId(item.getCollection()));
    } catch (Exception e) {
      BeanHelper.error("There has been an error during saving of the item: " + e.getMessage());
      LOGGER.error("Error single upload: ", e);
    }
    sus.reset();
    return "";
  }

  /**
   * Reload the page with the current user
   *
   * @throws IOException
   */
  private void reloadItemPage(String itemIdString, String collectionIdString) {
    try {
      String redirectUrl = getNavigation().getCollectionUrl() + collectionIdString + "/"
          + getNavigation().getItemPath() + "/" + itemIdString;

      redirect(redirectUrl);
    } catch (IOException e) {
      Logger.getLogger(UserBean.class).info("Error reloading the page", e);
    }
  }


  /**
   * After the file has been uploaded
   *
   * @throws Exception
   */
  private void prepareEditor() throws Exception {
    StorageController sc = new StorageController();
    if (sc.guessNotAllowedFormat(sus.getIngestImage().getFile()).equals(StorageUtils.BAD_FORMAT)) {
      sus.reset();
      throw new TypeNotAllowedException(
          Imeji.RESOURCE_BUNDLE.getMessage("single_upload_invalid_content_format", getLocale()));
    }
    sus.copyToTemp();
  }

  private Item uploadFileToItem(Item item, CollectionImeji col, File file, String title)
      throws ImejiException {
    ItemBusinessController controller = new ItemBusinessController();
    item = controller.create(item, col, file, title, getSessionUser(), null, null);
    sus.setUploadedItem(item);
    return item;
  }

  /**
   * Upload the file and read the technical Metadata
   *
   * @throws FileUploadException
   * @throws TypeNotAllowedException
   */
  public void upload() throws FileUploadException, TypeNotAllowedException {
    HttpServletRequest request =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    List<String> techMd = new ArrayList<String>();
    this.ingestImage = getUploadedIngestFile(request);
    sus.setIngestImage(ingestImage);
    techMd = TikaExtractor.extractFromFile(ingestImage.getFile());
    sus.setTechMD(techMd);

  }

  /**
   * Upload the file
   *
   * @param request
   * @return
   * @throws FileUploadException
   * @throws TypeNotAllowedException
   */
  private IngestImage getUploadedIngestFile(HttpServletRequest request)
      throws FileUploadException, TypeNotAllowedException {
    File tmp = null;
    boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    IngestImage ii = new IngestImage();
    if (isMultipart) {
      ServletFileUpload upload = new ServletFileUpload();
      try {
        FileItemIterator iter = upload.getItemIterator(request);
        while (iter.hasNext()) {
          FileItemStream fis = iter.next();
          String filename = fis.getName();
          InputStream in = fis.openStream();
          tmp = TempFileUtil.createTempFile("singleupload",
              "." + FilenameUtils.getExtension(filename));
          FileOutputStream fos = new FileOutputStream(tmp);
          if (!fis.isFormField()) {
            try {
              IOUtils.copy(in, fos);
            } finally {
              in.close();
              fos.close();
              ii.setName(filename);
            }

          }
        }
        ii.setFile(tmp);
      } catch (IOException | FileUploadException e) {
        LOGGER.info("Could not get uploaded ingest file", e);
      }
    }
    return ii;
  }

  public void colChangeListener(AjaxBehaviorEvent event) throws Exception {
    methodColChangeListener();
  }

  private void methodColChangeListener() throws ImejiException {
    if (!"".equals(selectedCollectionItem)) {
      sus.setSelectedCollectionItem(selectedCollectionItem);

      CollectionImeji collection = new CollectionController()
          .retrieveLazy(URI.create(selectedCollectionItem), getSessionUser());
      MetadataProfile profile =
          new ProfileController().retrieve(collection.getProfile(), getSessionUser());
      ((SuggestBean) BeanHelper.getSessionBean(SuggestBean.class)).init(profile);

      MetadataSet mdSet = profile != null ? ImejiFactory.newMetadataSet(profile.getId())
          : ImejiFactory.newMetadataSet(null);
      MetadataSetWrapper mdSetBean = new MetadataSetWrapper(mdSet, profile, true);
      metadataLabels = new MetadataLabels(profile, getLocale());
      sus.setCollection(collection);
      sus.setProfile(profile);
      sus.setMdSetBean(mdSetBean);
      licenseEditor =
          new LicenseEditor(getLocale(), collection.getStatus().equals(Status.RELEASED));
    }
  }

  /**
   * Add a Metadata of the same type as the passed metadata
   */
  public void addMetadata(MetadataWrapper md) {
    MetadataWrapper newMd = md.copyEmpty();
    newMd.addEmtpyChilds(sus.getProfile());
    sus.getMdSetBean().getTree().add(newMd);
  }

  /**
   * Remove the active metadata
   */
  public void removeMetadata(MetadataWrapper smb) {
    sus.getMdSetBean().getTree().remove(smb);
    sus.getMdSetBean().addEmtpyValues();
  }

  /**
   * Load the collections where the user can upload the file
   *
   * @throws ImejiException
   */
  private void loadCollections() throws ImejiException {
    for (CollectionImeji c : retrieveAllUserCollections()) {
      if (SecurityUtil.staticAuth().createContent(getSessionUser(), c)) {
        collectionItems.add(new SelectItem(c.getId(), c.getMetadata().getTitle()));
      }
    }
    // If the user hasn't any collection but is allowed to create one, create a default collection
    if (collectionItems.isEmpty() && SecurityUtil.isAllowedToCreateCollection(getSessionUser())) {
      CollectionImeji defaultCollection = createDefaultCollection();
      collectionItems.add(
          new SelectItem(defaultCollection.getId(), defaultCollection.getMetadata().getTitle()));
    }
    // If there is no collection where the user can upload, send error
    if (collectionItems.isEmpty()) {
      throw new BadRequestException(
          Imeji.RESOURCE_BUNDLE.getMessage("cannot_create_collection", getLocale()));
    } else if (collectionItems.size() >= 1) {
      collectionItems.add(0, new SelectItem("", "-- Select a collection to upload your file --"));
    }
    setSelectedCollection(collectionItems.get(0).getValue().toString());
  }


  /**
   * Retrieve all the collections which the current user can read
   *
   * @return
   * @throws ImejiException
   */
  private List<CollectionImeji> retrieveAllUserCollections() throws ImejiException {
    CollectionController cc = new CollectionController();
    SearchQuery sq = new SearchQuery();
    SortCriterion sortCriterion =
        new SortCriterion(new SearchIndex(SearchFields.title), SortOrder.ASCENDING);
    SearchResult results =
        cc.search(sq, sortCriterion, -1, 0, getSessionUser(), selectedSpaceString);
    return (List<CollectionImeji>) cc.retrieveBatchLazy(results.getResults(), -1, 0,
        getSessionUser());
  }

  /**
   * Create a default collection where the user can upload his files
   *
   * @throws ImejiException
   */
  private CollectionImeji createDefaultCollection() throws ImejiException {
    CollectionController cc = new CollectionController();
    CollectionImeji newC = ImejiFactory.newCollection();
    newC.getMetadata()
        .setTitle("Default first collection of " + getSessionUser().getPerson().getCompleteName());

    Person creatorUser = getSessionUser().getPerson();

    // If there are no organizations for Current User, add one
    if ("".equals(creatorUser.getOrganizationString())) {
      Organization creatorOrganization = new Organization();
      creatorUser.getOrganizations().clear();
      creatorOrganization.setName("Organization name not specified");
      creatorUser.getOrganizations().add(creatorOrganization);
    }

    // ImejiFactory initiates new Empty Person, which is not needed
    newC.getMetadata().getPersons().clear();
    // Add current user as Author
    newC.getMetadata().getPersons().add(creatorUser);

    MetadataProfileBusinessController metadataProfileBC = new MetadataProfileBusinessController();
    newC.setProfile(metadataProfileBC.retrieveDefaultProfile().getId());
    return cc.create(newC, metadataProfileBC.retrieveDefaultProfile(), getSessionUser(),
        MetadataProfileCreationMethod.REFERENCE, selectedSpaceString);
  }

  public List<SelectItem> getCollectionItems() {
    return collectionItems;
  }

  public void setCollectionItems(List<SelectItem> collectionItems) {
    this.collectionItems = collectionItems;
  }

  public String getSelectedCollectionItem() {
    return sus.getSelectedCollectionItem();
  }

  public void setSelectedCollection(String selectedCollectionItem) {
    this.selectedCollectionItem = selectedCollectionItem;
  }

  public void setSelectedCollectionItem(String selectedCollectionItem) {
    this.selectedCollectionItem = selectedCollectionItem;
  }

  public CollectionImeji getCollection() {
    return sus.getCollection();
  }

  public MetadataSetWrapper getMdSetBean() {
    return sus.getMdSetBean();
  }

  public List<String> getTechMd() {
    return sus.getTechMD();
  }

  public SingleUploadSession getSus() {
    return sus;
  }

  public void setSus(SingleUploadSession sus) {
    this.sus = sus;
  }

  public MetadataLabels getLabels() {
    return sus.getLabels();
  }

  public IngestImage getIngestImage() {
    return sus.getIngestImage();
  }

  public String getfFile() {
    return sus.getfFile();
  }

  public Item getItem() {
    return sus.getUploadedItem();
  }

  public static String extractIDFromURI(URI uri) {
    return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
  }

  public boolean readyForUploading() {
    return sus.isUploadFileToTemp() && sus.getCollection() != null;
  }

  public MetadataLabels getMetadataLabels() {
    return metadataLabels;
  }

  public String getSelectedSpaceString() {
    return selectedSpaceString;
  }

  public void setSelectedSpaceString(String selectedSpaceString) {
    this.selectedSpaceString = selectedSpaceString;
  }

  public boolean isHasUploadRights() {
    return hasUploadRights;
  }

  public void setHasUploadRights(boolean hasUploadRights) {
    this.hasUploadRights = hasUploadRights;
  }

  /**
   * @return the licenseEditor
   */
  public LicenseEditor getLicenseEditor() {
    return licenseEditor;
  }

  /**
   * @param licenseEditor the licenseEditor to set
   */
  public void setLicenseEditor(LicenseEditor licenseEditor) {
    this.licenseEditor = licenseEditor;
  }
}
