/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import static de.mpg.imeji.logic.notification.CommonMessages.getSuccessCollectionDeleteMessage;

import java.net.URI;
import java.util.Collection;

import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.CollectionController;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerBean.CONTAINER_TYPE;
import de.mpg.imeji.presentation.image.ThumbnailBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.CommonUtils;
import de.mpg.imeji.presentation.util.ObjectLoader;

/**
 * Item of the collections page.
 * 
 * @author saquet
 */
public class CollectionListItem {
  private String title = "";
  private String description = "";
  private String descriptionFull = "";
  private String authors = "";
  private int size = 0;
  private String status = Status.PENDING.toString();
  private String id = null;
  private URI uri = null;
  private String discardComment = "";
  private boolean selected = false;
  // dates
  private String creationDate = null;
  private String lastModificationDate = null;
  private String versionDate = null;
  private static final Logger LOGGER = Logger.getLogger(CollectionListItem.class);
  private ThumbnailBean thumbnail = null;
  private String selectedGrant;
  private URI profileURI;
  private boolean isOwner = false;
  /**
   * Maximum number of character displayed in the list for the description
   */
  private static final int DESCRIPTION_MAX_SIZE = 330; // 430

  /**
   * Construct a new {@link CollectionListItem} with a {@link CollectionImeji}
   * 
   * @param collection
   * @param user
   */
  public CollectionListItem(CollectionImeji collection, User user) {
    try {
      title = collection.getMetadata().getTitle();
      description = CommonUtils.removeTags(collection.getMetadata().getDescription());
      descriptionFull = description;
      if (description != null && description.length() > DESCRIPTION_MAX_SIZE) {
        description = description.substring(0, DESCRIPTION_MAX_SIZE) + "...";
      }
      for (Person p : collection.getMetadata().getPersons()) {
        if (!"".equals(authors)) {
          authors += "; ";
        }
        authors += p.getFamilyName() + ", " + p.getGivenName();
      }
      profileURI = collection.getProfile();
      uri = collection.getId();
      setId(ObjectHelper.getId(uri));
      status = collection.getStatus().toString();
      discardComment = collection.getDiscardComment();
      creationDate = collection.getCreated().getTime().toString();
      lastModificationDate = collection.getModified().getTime().toString();
      if (collection.getVersionDate() != null) {
        versionDate = collection.getVersionDate().getTime().toString();
      }
      // initializations
      initThumbnail(collection, user);
      initSize(collection, user);
      // initDiscardedSize(collection, user);
      initSelected();
      if (user != null) {
        isOwner = collection.getCreatedBy().equals(user.getId());
      }
    } catch (Exception e) {
      LOGGER.error("Error creating collectionListItem", e);
    }
  }

  /**
   * Find the Logo of the collection. If no logo defined, use the first file of the collection
   * 
   * @param collection
   * @param user
   * @throws ImejiException
   * @throws Exception
   */
  private void initThumbnail(CollectionImeji collection, User user)
      throws ImejiException, Exception {
    if (collection.getLogoUrl() != null) {
      thumbnail = new ThumbnailBean();
      thumbnail.setLink(collection.getLogoUrl().toString());
    } else {
      ItemController ic = new ItemController();
      Container searchedContainer = ic.searchAndSetContainerItems(collection, user, 1, 0);
      if (searchedContainer.getImages().iterator().hasNext()) {
        URI uri = searchedContainer.getImages().iterator().next();
        if (uri != null) {
          this.thumbnail = new ThumbnailBean(ic.retrieveLazy(uri, user), false);
        }
      }
    }
  }

  /**
   * Count the size of the collection
   * 
   * @param user
   */
  private void initSize(CollectionImeji collection, User user) {
    ItemController ic = new ItemController();
    size =
        ic.search(collection.getId(), null, null, Imeji.adminUser, null, 0, 0).getNumberOfRecords();
  }


  /**
   * Chek if the {@link CollectionImeji} is selected in the {@link SessionBean}
   */
  private void initSelected() {
    if (((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getSelectedCollections()
        .contains(uri))
      selected = true;
    else
      selected = false;
  }

  /**
   * Release the {@link Collection} in the list
   * 
   * @return
   */
  public String release() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    CollectionController cc = new CollectionController();
    try {
      cc.release(cc.retrieve(uri, sessionBean.getUser()), sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_collection_release"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_collection_release"));
      BeanHelper.error(sessionBean.getMessage(e.getMessage()));
      LOGGER.error(sessionBean.getMessage("error_collection_release"), e);
    }
    return "pretty:";
  }

  /**
   * Delete the {@link CollectionImeji} in the list
   * 
   * @return
   */
  public String delete() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    CollectionController cc = new CollectionController();
    try {
      CollectionImeji c = cc.retrieve(uri, sessionBean.getUser());
      cc.delete(c, sessionBean.getUser());
      BeanHelper.info(getSuccessCollectionDeleteMessage(c.getMetadata().getTitle(), sessionBean));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_collection_delete") + ":" + e.getMessage());
      LOGGER.error(sessionBean.getMessage("error_collection_delete"), e);
    }
    return ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
        .getPrettySpacePage("pretty:collections");
  }

  public String createDOI() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    try {
      String doi = UrlHelper.getParameterValue("doi");
      CollectionImeji collection = new CollectionController().retrieve(uri, sessionBean.getUser());
      DoiService doiService = new DoiService();
      if (doi != null) {
        doiService.addDoiToCollection(doi, collection, sessionBean.getUser());
      } else {
        doiService.addDoiToCollection(collection, sessionBean.getUser());
      }
      BeanHelper.info(sessionBean.getMessage("success_doi_creation"));
    } catch (ImejiException e) {
      BeanHelper.error(sessionBean.getMessage("error_doi_creation") + " " + e.getMessage());
      LOGGER.error("Error during doi creation", e);
    }
    return "pretty:";
  }

  /**
   * Withdraw the {@link CollectionImeji} of the list
   * 
   * @return
   */
  public String withdraw() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    try {
      CollectionController cc = new CollectionController();
      CollectionImeji c = ObjectLoader.loadCollectionLazy(uri, sessionBean.getUser());
      c.setDiscardComment(getDiscardComment());
      cc.withdraw(c, sessionBean.getUser());
      BeanHelper.info(sessionBean.getMessage("success_collection_withdraw"));
    } catch (Exception e) {
      BeanHelper.error(sessionBean.getMessage("error_collection_withdraw"));
      BeanHelper.error(e.getMessage());
      LOGGER.error(sessionBean.getMessage("error_collection_withdraw"), e);
    }
    return "pretty:";
  }

  /**
   * Listener for the discard comment
   * 
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
      setDiscardComment(event.getNewValue().toString().trim());
    }
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getAuthors() {
    return authors;
  }

  public void setAuthors(String authors) {
    this.authors = authors;
  }

  public int getSize() {
    return size;
  }

  public void setSize(int size) {
    this.size = size;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public String getDiscardComment() {
    return discardComment;
  }

  public void setDiscardComment(String discardComment) {
    this.discardComment = discardComment;
  }

  public String getCreationDate() {
    return creationDate;
  }

  public void setCreationDate(String creationDate) {
    this.creationDate = creationDate;
  }

  public String getLastModificationDate() {
    return lastModificationDate;
  }

  public void setLastModificationDate(String lastModificationDate) {
    this.lastModificationDate = lastModificationDate;
  }

  public String getVersionDate() {
    return versionDate;
  }

  public void setVersionDate(String versionDate) {
    this.versionDate = versionDate;
  }

  public boolean isSelected() {
    return selected;
  }

  public void setSelected(boolean selected) {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (selected) {
      if (!(sessionBean.getSelectedCollections().contains(uri))) {
        sessionBean.getSelectedCollections().add(uri);
      }
    } else
      sessionBean.getSelectedCollections().remove(uri);
    this.selected = selected;
  }

  public URI getUri() {
    return uri;
  }

  public void setUri(URI uri) {
    this.uri = uri;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public ThumbnailBean getThumbnail() {
    return thumbnail;
  }

  public void setThumbnail(ThumbnailBean thumbnail) {
    this.thumbnail = thumbnail;
  }

  public String getDescriptionFull() {
    return descriptionFull;
  }

  public void setDescriptionFull(String descriptionFull) {
    this.descriptionFull = descriptionFull;
  }

  public String getSelectedGrant() {
    return selectedGrant;
  }

  public void setSelectedGrant(String selectedGrant) {
    this.selectedGrant = selectedGrant;
  }

  public URI getProfileURI() {
    return profileURI;
  }

  public void setProfileURI(URI profileURI) {
    this.profileURI = profileURI;
  }

  public boolean isOwner() {
    return isOwner;
  }

  public void setOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  public String getType() {
    return CONTAINER_TYPE.COLLECTION.name();
  }

}
