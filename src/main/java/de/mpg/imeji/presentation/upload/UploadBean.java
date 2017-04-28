package de.mpg.imeji.presentation.upload;

import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.AND;
import static de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS.OR;

import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.authorization.Authorization;
import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "UploadBean")
@ViewScoped
public class UploadBean extends SuperBean {
  private static final long serialVersionUID = 4632180647351059603L;
  private static final Logger LOGGER = Logger.getLogger(UploadBean.class);
  private List<CollectionImeji> collections = new ArrayList<>();
  private String query = "";

  public UploadBean() {

  }

  public void init() {
    try {
      if (getSessionUser() != null) {
        filterCollections();
        if (collections.isEmpty()
            && SecurityUtil.authorization().hasCreateCollectionGrant(getSessionUser())) {
          redirect(getNavigation().getCreateCollectionUrl() + "?showUpload=1");
        } else if (collections.isEmpty()
            && !SecurityUtil.authorization().hasCreateCollectionGrant(getSessionUser())) {
          BeanHelper
              .error(Imeji.RESOURCE_BUNDLE.getMessage("cannot_create_collection", getLocale()));
        }
      }
    } catch (Exception e) {
      BeanHelper.error("Error initializing page: " + e.getMessage());
      // LOGGER.error("Error initializing upload page", e);
    }
  }

  public void filterCollections() throws ImejiException {
    SearchFactory factory = new SearchFactory();
    factory.addElement(new SearchPair(SearchFields.title, query + "*"), OR);
    if (!new Authorization().isSysAdmin(getSessionUser())) {
      factory.addElement(new SearchPair(SearchFields.role, GrantType.EDIT.name().toLowerCase()),
          AND);
    }
    setCollections(
        new CollectionService().searchAndRetrieve(factory.build(), null, getSessionUser(), -1, 0));
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

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @param query the query to set
   */
  public void setQuery(String query) {
    this.query = query;
  }

}
