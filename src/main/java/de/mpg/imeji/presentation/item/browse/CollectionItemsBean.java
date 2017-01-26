package de.mpg.imeji.presentation.item.browse;

import java.net.URI;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.presentation.collection.CollectionActionMenu;
import de.mpg.imeji.presentation.facet.FacetsJob;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * {@link ItemsBean} to browse {@link Item} of a {@link CollectionImeji}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CollectionItemsBean")
@ViewScoped
public class CollectionItemsBean extends ItemsBean {
  private static final long serialVersionUID = 2506992231592053506L;
  private String id = null;
  private URI uri;
  private CollectionImeji collection;
  private SearchQuery searchQuery = new SearchQuery();
  private CollectionActionMenu actionMenu;
  private String authors = "";
  private int size;

  /**
   * Initialize the bean
   *
   * @throws ImejiException
   */
  public CollectionItemsBean() {
    super();
  }

  @Override
  public void initSpecific() {
    try {
      id = UrlHelper.getParameterValue("id");
      uri = ObjectHelper.getURI(CollectionImeji.class, id);
      collection = new CollectionService().retrieveLazy(uri, getSessionUser());
      browseContext = getNavigationString() + id;
      update();
      actionMenu = new CollectionActionMenu(collection, getSessionUser(), getLocale());
      collection.getPersons().stream().map(p -> p.AsFullText())
          .forEach(a -> authors = authors.equals("") ? a : "; " + a);
      size = StringHelper.isNullOrEmptyTrim(getQuery()) ? getTotalNumberOfRecords()
          : getCollectionSize();
    } catch (final Exception e) {
      LOGGER.error("Error initializing collectionItemsBean", e);
    }
  }

  /**
   * Copy the items from clipboard in this collection
   */
  public void paste() {
    try {
      new ItemService().copyItems(new ArrayList<>(getSessionBean().getClipboard()), collection,
          getSessionUser());
    } catch (ImejiException e) {
      BeanHelper.error("Error copying items " + e.getMessage());
      LOGGER.error("Error copying items ", e);
    }
  }

  /**
   * Move the items from clipboard in this collection
   */
  public void move() {
    try {
      new ItemService().moveItems(new ArrayList<>(getSessionBean().getClipboard()), collection,
          getSessionUser());
    } catch (ImejiException e) {
      BeanHelper.error("Error moving items " + e.getMessage());
      LOGGER.error("Error moving items ", e);
    }
  }

  private int getCollectionSize() {
    return new ItemService().search(collection.getId(), null, null, Imeji.adminUser, 1, -1)
        .getNumberOfRecords();
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCriterion, int offset,
      int limit) {
    final ItemService controller = new ItemService();
    return controller.search(uri, searchQuery, sortCriterion, getSessionUser(), limit, offset);
  }


  @Override
  public String getNavigationString() {
    return "pretty:collectionBrowse";
  }

  @Override
  public void initFacets() {
    try {
      searchQuery = SearchQueryParser.parseStringQuery(getQuery());
      final SearchResult searchRes = search(getSearchQuery(), null, 0, -1);
      setFacets(new FacetsJob(collection, searchQuery, searchRes, getSessionUser(), getLocale()));
      final ExecutorService executor = Executors.newSingleThreadScheduledExecutor();
      executor.submit(getFacets());
      executor.shutdown();
    } catch (final Exception e) {
      LOGGER.error("Error initialising the facets", e);
    }
  }

  /**
   * return the url of the collection
   */
  @Override
  public String getImageBaseUrl() {
    if (collection == null) {
      return "";
    }
    return getNavigation().getApplicationUrl() + "collection/" + this.id + "/";
  }

  /**
   * return the url of the collection
   */
  @Override
  public String getBackUrl() {
    return getNavigation().getBrowseUrl() + "/collection" + "/" + this.id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
    // @Ye set session value to share with CollectionItemsBean, another way is via injection
    FacesContext.getCurrentInstance().getExternalContext().getSessionMap()
        .put("CollectionItemsBean.id", id);
  }

  public void setCollection(CollectionImeji collection) {
    this.collection = collection;
  }

  public CollectionImeji getCollection() {
    return collection;
  }


  @Override
  public String getType() {
    return PAGINATOR_TYPE.COLLECTION_ITEMS.name();
  }

  /**
   * @return the actionMenu
   */
  public CollectionActionMenu getActionMenu() {
    return actionMenu;
  }

  /**
   * @param actionMenu the actionMenu to set
   */
  public void setActionMenu(CollectionActionMenu actionMenu) {
    this.actionMenu = actionMenu;
  }

  @Override
  public String getCollectionId() {
    return collection.getId().toString();
  }

  public String getAuthors() {
    return authors;
  }

  public String getCitation() {
    final String url = getDoiUrl().isEmpty() ? getPageUrl() : getDoiUrl();
    return authors + ". " + collection.getTitle() + ". <a href=\"" + url + "\">" + url + "</a>";
  }

  /**
   * The Url to view the DOI
   *
   * @return
   */
  public String getDoiUrl() {
    return collection.getDoi().isEmpty() ? "" : DoiService.DOI_URL_RESOLVER + collection.getDoi();
  }

  public String getPageUrl() {
    return getNavigation().getCollectionUrl() + id;
  }

  public int getSize() {
    return size;
  }

}

