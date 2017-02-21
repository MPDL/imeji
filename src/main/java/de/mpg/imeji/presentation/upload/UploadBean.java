package de.mpg.imeji.presentation.upload;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "UploadBean")
@ViewScoped
public class UploadBean extends SuperBean {
  private static final long serialVersionUID = 4632180647351059603L;
  private final String uploadId;
  private String collectionId;
  private List<CollectionImeji> collections = new ArrayList<>();

  public UploadBean() {
    uploadId = IdentifierUtil.newUniversalUniqueId();
  }

  @PostConstruct
  public void init() {
    try {
      if (UrlHelper.hasParameter("col")
          && !StringHelper.isNullOrEmptyTrim(UrlHelper.getParameterValue("col"))) {
        collectionId = ObjectHelper
            .getURI(CollectionImeji.class, UrlHelper.getParameterValue("col")).toString();
      } else {
        SearchFactory factory = new SearchFactory();
        if (!new Authorization().isSysAdmin(getSessionUser())) {
          factory.addElement(new SearchPair(SearchFields.role, GrantType.EDIT.name().toLowerCase()),
              AND);
        }
        setCollections(new CollectionService().searchAndRetrieve(factory.build(), null,
            getSessionUser(), -1, 0));
        if (!collections.isEmpty()) {
          collectionId = collections.get(0).getId().toString();
        }
      }
    } catch (Exception e) {
      BeanHelper.error("Error initializing page: " + e.getMessage());
    }
  }

  /**
   * Create the data which have been uploaded to the staging area with the current uploadId
   * 
   * @throws IOException
   */
  public void create() {
    try {
      new ItemService().createFromStaging(uploadId, getSessionUser());
    } catch (ImejiException e) {
      BeanHelper.error("Error upload " + e.getMessage());
    }
  }

  public String getCollectionUrl() {
    return getNavigation().getCollectionPath() + "/" + ObjectHelper.getId(URI.create(collectionId));
  }

  /**
   * @return the uploadId
   */
  public String getUploadId() {
    return uploadId;
  }

  /**
   * @return the collectionId
   */
  public String getCollectionId() {
    return collectionId;
  }

  /**
   * @param collectionId the collectionId to set
   */
  public void setCollectionId(String collectionId) {
    this.collectionId = collectionId;
  }

  /**
   * @return the collections
   */
  public List<CollectionImeji> getCollections() {
    return collections;
  }

  /**
   * @param collections the collections to set
   */
  public void setCollections(List<CollectionImeji> collections) {
    this.collections = collections;
  }

}
