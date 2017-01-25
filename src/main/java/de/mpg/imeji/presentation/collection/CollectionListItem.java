/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.collection;

import java.io.Serializable;
import java.net.URI;

import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.ContainerBean.CONTAINER_TYPE;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CommonUtils;

/**
 * Item of the collections page.
 *
 * @author saquet
 */
public class CollectionListItem implements Serializable {
  private static final long serialVersionUID = -782035871566935720L;
  private static final Logger LOGGER = Logger.getLogger(CollectionListItem.class);
  private String title = "";
  private String description = "";
  private String descriptionFull = "";
  private String authors = "";
  private int size = 0;
  private String status = Status.PENDING.toString();
  private String id = null;
  private URI uri = null;
  private String discardComment = "";
  private String lastModificationDate = null;
  private String selectedGrant;
  private boolean isOwner = false;
  private CollectionImeji collection;
  private String logoUrl;
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
      this.collection = collection;
      title = collection.getTitle();
      description = CommonUtils.removeTags(collection.getDescription());
      descriptionFull = description;
      if (description != null && description.length() > DESCRIPTION_MAX_SIZE) {
        description = description.substring(0, DESCRIPTION_MAX_SIZE) + "...";
      }
      for (final Person p : collection.getPersons()) {
        if (!"".equals(authors)) {
          authors += "; ";
        }
        authors += p.getFamilyName() + ", " + p.getGivenName();
      }
      uri = collection.getId();
      setId(ObjectHelper.getId(uri));
      status = collection.getStatus().toString();
      discardComment = collection.getDiscardComment();
      // creationDate = collection.getCreated().getTime().toString();
      lastModificationDate = collection.getModified().getTime().toString();
      // initializations
      initLogo(collection, user);
      initSize(collection, user);
      if (user != null) {
        isOwner = collection.getCreatedBy().equals(user.getId());
      }
    } catch (final Exception e) {
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
  private void initLogo(CollectionImeji collection, User user) throws ImejiException, Exception {
    if (collection.getLogoUrl() != null) {
      this.logoUrl = collection.getLogoUrl().toString();
    } else {
      final ItemService itemService = new ItemService();
      final Container searchedContainer =
          itemService.searchAndSetContainerItems(collection, user, 1, 0);
      if (searchedContainer.getImages().iterator().hasNext()) {
        final URI uri = searchedContainer.getImages().iterator().next();
        if (uri != null) {
          final Navigation navigation =
              (Navigation) BeanHelper.getApplicationBean(Navigation.class);
          String contentId = new ContentService().findContentId(uri.toString());
          this.logoUrl =
              navigation.getFileUrl() + "?content=" + contentId + "&amp;resolution=thumbnail";
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
    final ItemService ic = new ItemService();
    size = ic.search(collection.getId(), null, null, Imeji.adminUser, 0, 0).getNumberOfRecords();
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

  public String getLastModificationDate() {
    return lastModificationDate;
  }

  public void setLastModificationDate(String lastModificationDate) {
    this.lastModificationDate = lastModificationDate;
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

  public boolean isOwner() {
    return isOwner;
  }

  public void setOwner(boolean isOwner) {
    this.isOwner = isOwner;
  }

  public String getType() {
    return CONTAINER_TYPE.COLLECTION.name();
  }

  public CollectionImeji getCollection() {
    return collection;
  }

  public String getLogoUrl() {
    return logoUrl;
  }
}
