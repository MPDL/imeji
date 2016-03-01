package de.mpg.imeji.presentation.upload;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

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
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.CollectionController.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.controller.ProfileController;
import de.mpg.imeji.logic.controller.exceptions.TypeNotAllowedException;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
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
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.metadata.MetadataSetBean;
import de.mpg.imeji.presentation.metadata.SingleEditBean;
import de.mpg.imeji.presentation.metadata.SuperMetadataBean;
import de.mpg.imeji.presentation.metadata.extractors.TikaExtractor;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.user.UserBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.presentation.util.ObjectLoader;

@ManagedBean(name = "SingleUploadBean")
@ViewScoped
public class SingleUploadBean implements Serializable {
  private static final long serialVersionUID = -2731118794797476328L;
  private static final Logger LOGGER = Logger.getLogger(SingleUploadBean.class);

  // private Collection<CollectionImeji> collections = new ArrayList<CollectionImeji>();

  private List<SelectItem> collectionItems = new ArrayList<SelectItem>();
  private String selectedCollectionItem;

  @ManagedProperty("#{SingleUploadSession}")
  private SingleUploadSession sus;

  @ManagedProperty("#{SessionBean}")
  private SessionBean sb;

  @ManagedProperty(value = "#{SessionBean.user}")
  private User user;

  private IngestImage ingestImage;

  public SingleUploadBean() {}

  public void init() throws IOException {
    if (user != null && user.isAllowedToCreateCollection()) {
      try {
        if (UrlHelper.getParameterBoolean("init")) {
          sus.reset();
          isAllowedToUpload();
        } else if (UrlHelper.getParameterBoolean("start")) {
          // loadCollections(false);
          upload();
          // loadCollections(true);
        } else if (UrlHelper.getParameterBoolean("done") && !UrlHelper.hasParameter("h")) {
          // loadCollections(false);
          loadCollections();
          prepareEditor();
        }
      } catch (Exception e) {
        BeanHelper.error(e.getLocalizedMessage());
      }
    } else {
      if (user != null) {
        BeanHelper.cleanMessages();
        BeanHelper.info("You have no right to create collections, thus you can not upload items!");
        Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
        FacesContext.getCurrentInstance().getExternalContext().redirect(navigation.getHomeUrl());

      }
    }
  }

  public String save() {
    try {
      Item item = ImejiFactory.newItem(getCollection());
      SingleEditBean edit = new SingleEditBean(item, sus.getProfile());
      MetadataSetBean newSet = getMdSetBean();
      edit.getEditor().getItems().get(0).setMds(newSet);
      edit.getEditor().validateAndFormatItemsForSaving();
      uploadFileToItem(item, getIngestImage().getFile(), getIngestImage().getName());
      sus.uploaded();
      BeanHelper.cleanMessages();
      reloadItemPage(item.getIdString(), ObjectHelper.getId(item.getCollection()));
    } catch (Exception e) {
      BeanHelper.error("There has been an error during saving of the item: " + e.getMessage());
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
      Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);

      String redirectUrl = navigation.getCollectionUrl() + collectionIdString + "/"
          + navigation.getItemPath() + "/" + itemIdString;

      FacesContext.getCurrentInstance().getExternalContext().redirect(redirectUrl);
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
      throw new TypeNotAllowedException(sb.getMessage("single_upload_invalid_content_format"));
    }
    sus.copyToTemp();
  }

  private Item uploadFileToItem(Item item, File file, String title) throws ImejiException {

    ItemController controller = new ItemController();
    item = controller.create(item, file, title, user, null, null);
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
      try {
        CollectionImeji collection =
            ObjectLoader.loadCollectionLazy(new URI(selectedCollectionItem), user);
        MetadataProfile profile = ObjectLoader.loadProfile(collection.getProfile(), user);
        ((SuggestBean) BeanHelper.getSessionBean(SuggestBean.class)).init(profile);

        MetadataSet mdSet = profile != null ? ImejiFactory.newMetadataSet(profile.getId())
            : ImejiFactory.newMetadataSet(null);
        MetadataSetBean mdSetBean = new MetadataSetBean(mdSet, profile, true);
        MetadataLabels labels = (MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class);
        labels.init(profile);
        sus.setCollection(collection);
        sus.setProfile(profile);
        sus.setMdSetBean(mdSetBean);
      } catch (URISyntaxException e) {
        LOGGER.info("Pure URI Syntax issue ", e);
      }
    } else {

    }
  }

  /**
   * Add a Metadata of the same type as the passed metadata
   */
  public void addMetadata(SuperMetadataBean smb) {
    SuperMetadataBean newMd = smb.copyEmpty();
    newMd.addEmtpyChilds(sus.getProfile());
    sus.getMdSetBean().getTree().add(newMd);
  }

  /**
   * Remove the active metadata
   */
  public void removeMetadata(SuperMetadataBean smb) {
    sus.getMdSetBean().getTree().remove(smb);
    sus.getMdSetBean().addEmtpyValues();
  }

  /**
   * Check if the user has at right to upload in at least one collection. If not, check if the user
   * can create a collection
   * 
   * @throws ImejiException
   */
  private void isAllowedToUpload() throws ImejiException {
    for (CollectionImeji c : retrieveAllUserCollections()) {
      if (AuthUtil.staticAuth().createContent(user, c)) {
        collectionItems.add(new SelectItem(c.getId(), c.getMetadata().getTitle()));
      }
    }
    if (collectionItems.isEmpty() && !user.isAllowedToCreateCollection()) {
      sus.setCanUpload(false);
      throw new BadRequestException(sb.getMessage("cannot_create_collection"));
    }
    sus.setCanUpload(true);
  }

  /**
   * Load the collections where the user can upload the file
   * 
   * @throws ImejiException
   */
  private void loadCollections() throws ImejiException {
    for (CollectionImeji c : retrieveAllUserCollections()) {
      if (AuthUtil.staticAuth().createContent(user, c)) {
        collectionItems.add(new SelectItem(c.getId(), c.getMetadata().getTitle()));
      }
    }
    // If the user hasn't any collection but is allowed to create one, create a default collection
    if (collectionItems.isEmpty() && user.isAllowedToCreateCollection()) {
      CollectionImeji defaultCollection = createDefaultCollection();
      collectionItems.add(
          new SelectItem(defaultCollection.getId(), defaultCollection.getMetadata().getTitle()));
    }
    // If there is no collection where the user can upload, send error
    if (collectionItems.isEmpty()) {
      throw new BadRequestException(sb.getMessage("cannot_create_collection"));
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
        new SortCriterion(new SearchIndex(SearchFields.title), SortOrder.DESCENDING);
    SearchResult results = cc.search(sq, sortCriterion, -1, 0, user, sb.getSelectedSpaceString());
    return (List<CollectionImeji>) cc.retrieveBatchLazy(results.getResults(), -1, 0, user);
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
        .setTitle("Default first collection of " + user.getPerson().getCompleteName());

    Person creatorUser = getUser().getPerson();

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

    ProfileController pc = new ProfileController();
    newC.setProfile(pc.retrieveDefaultProfile().getId());
    URI id = cc.create(newC, pc.retrieveDefaultProfile(), user,
        MetadataProfileCreationMethod.REFERENCE, sb.getSelectedSpaceString());
    newC.setId(id);
    return newC;
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

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  // public Collection<CollectionImeji> getCollections() {
  // return collections;
  // }
  //
  // public void setCollections(Collection<CollectionImeji> collections) {
  // this.collections = collections;
  // }

  public void setSelectedCollectionItem(String selectedCollectionItem) {
    this.selectedCollectionItem = selectedCollectionItem;
  }

  public CollectionImeji getCollection() {
    return sus.getCollection();
  }

  public MetadataSetBean getMdSetBean() {
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

  public SessionBean getSb() {
    return sb;
  }

  public void setSb(SessionBean sb) {
    this.sb = sb;
  }

  public static String extractIDFromURI(URI uri) {
    return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
  }

  public boolean readyForUploading() {
    return sus.isUploadFileToTemp() && sus.getCollection() != null;
  }

}
