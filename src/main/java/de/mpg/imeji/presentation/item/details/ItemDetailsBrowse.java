package de.mpg.imeji.presentation.item.details;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.item.browse.ItemsBean;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * Object for the browsing over the detail items
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ItemDetailsBrowse implements Serializable {
  private static final long serialVersionUID = -1627171360319925422L;
  private static final Logger LOGGER = LogManager.getLogger(ItemDetailsBrowse.class);
  private String query;
  private String facetQuery;
  private String filterQuery;
  private final String containerUri;
  private final int currentPosition;
  private final List<SortCriterion> sortCriteria;
  private static final int SIZE = 3;
  private Item currentItem = null;
  private String next = null;
  private String previous = null;

  /**
   * Object for the browsing over the detail items. The Browsing is based on a ItemsBean and the
   * current {@link Item}.
   *
   * @param imagesBean
   * @param item
   * @param type
   * @param containerId
   */
  public ItemDetailsBrowse(Item item, String type, String containerUri, User user) {
    try {
      this.query = URLEncoder.encode(UrlHelper.hasParameter("q") ? UrlHelper.getParameterValue("q") : "", "UTF-8");
      this.facetQuery = URLEncoder.encode(UrlHelper.hasParameter("fq") ? UrlHelper.getParameterValue("fq") : "", "UTF-8");
      this.filterQuery = URLEncoder.encode(UrlHelper.hasParameter("filter") ? UrlHelper.getParameterValue("filter") : "", "UTF-8");
    } catch (UnsupportedEncodingException e) {
      LOGGER.error("Error encoding facetQuery and/or query", e);
    }
    this.containerUri = containerUri;
    this.currentItem = item;
    this.sortCriteria = initSortCriteria();
    this.currentPosition = UrlHelper.hasParameter("pos") ? Integer.parseInt(UrlHelper.getParameterValue("pos")) : -1;
    final List<String> items = searchPreviousAndNextItem(user);
    try {
      init(items, type, containerUri);
    } catch (Exception e) {
      LOGGER.error("Error initializing browse item", e);
    }
  }

  /**
   * Search for items previous and after to the current item.
   *
   * @param user
   * @param spaceId
   * @return
   */
  private List<String> searchPreviousAndNextItem(User user) {
    final ItemService controller = new ItemService();
    if (query != null && currentPosition > -1) {
      try {
        return controller.searchWithMultiLevelSorting(containerUri != null ? URI.create(containerUri) : null, getSearchQuery(),
            sortCriteria, user, SIZE, getOffset()).getResults();
      } catch (final UnprocessableError e) {
        LOGGER.error("Error retrieving items", e);
      }
    }
    return new ArrayList<>();
  }

  /**
   * Return the searchquery according the search and facet queries.
   * 
   * @return
   * @throws UnprocessableError
   */
  private SearchQuery getSearchQuery() throws UnprocessableError {
    return new SearchFactory().and(new SearchGroup(SearchQueryParser.parseStringQuery(query).getElements()))
        .and(new SearchGroup(SearchQueryParser.parseStringQuery(facetQuery).getElements())).initFilter(filterQuery).build();
  }

  /**
   * Initialize sort criteria for items align this with ItemsBean.getSortCriteriaForItems()
   *
   * @return
   */
  private List<SortCriterion> initSortCriteria() {

    List<SortCriterion> itemsSortCriteria = new LinkedList<SortCriterion>();

    // read first-level sort criterion from cookies:
    final String sortFieldName = CookieUtils.readNonNull(ItemsBean.ITEM_SORT_COOKIE, SearchFields.modified.name());
    final String orderFieldName = CookieUtils.readNonNull(ItemsBean.ITEM_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name());
    SortCriterion userSetSortCriterion = new SortCriterion(SearchFields.valueOfIndex(sortFieldName), SortOrder.valueOf(orderFieldName));
    // get second-level sort criterion:
    SortCriterion sortByFilenameAscending = ItemsBean.getSortByFilenameAscendingSortCriterion();

    itemsSortCriteria.add(userSetSortCriterion);
    itemsSortCriteria.add(sortByFilenameAscending);

    return itemsSortCriteria;

  }

  /**
   * Return the Offset for the saerch according to the current position
   *
   * @return
   */
  private int getOffset() {
    return currentPosition > 1 ? currentPosition - 1 : 0;
  }

  /**
   * Initialize the {@link ItemDetailsBrowse} for the current {@link Item} according to:
   *
   * @param type - if the detail page is initialized within a collection, an album, or a browse page
   *        (item)
   * @param path - the id (not the uri) of the current container ({@link Album} or
   *        {@link CollectionImeji})
   * @throws UnsupportedEncodingException
   */
  public void init(List<String> items, String type, String containerUri) throws UnsupportedEncodingException {
    String baseUrl = new String();
    if (type == "collection") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getCollectionUrl()
          + ObjectHelper.getId(URI.create(containerUri)) + "/item/";
    } else if (type == "item") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getItemUrl();
    } else if (type == "album") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getAlbumUrl() + ObjectHelper.getId(URI.create(containerUri))
          + "/item/";
    }
    final String nextItem = nextItem(items);
    final String previousItem = previousItem(items);
    if (nextItem != null) {
      next = baseUrl + ObjectHelper.getId(URI.create(nextItem)) + "?q=" + query + "&fq=" + facetQuery + "&filter=" + filterQuery + "&pos="
          + (this.currentPosition + 1);
    }
    if (previousItem != null) {
      previous = baseUrl + ObjectHelper.getId(URI.create(previousItem)) + "?q=" + query + "&fq=" + facetQuery + "&filter=" + filterQuery
          + "&pos=" + (this.currentPosition - 1);
    }
  }

  /**
   * Return the item id next to the current item
   *
   * @param items
   * @return
   */
  private String nextItem(List<String> items) {
    final int currentIndex = items.indexOf(currentItem.getId().toString());
    return currentIndex < items.size() - 1 ? items.get(currentIndex + 1) : null;
  }

  /**
   * Return the item id previous to the current item
   *
   * @param items
   * @return
   */
  private String previousItem(List<String> items) {
    final int currentIndex = items.indexOf(currentItem.getId().toString());
    return currentIndex > 0 ? items.get(0) : null;
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }

  public String getPrevious() {
    return previous;
  }

  public void setPrevious(String previous) {
    this.previous = previous;
  }

  /**
   * @return the query
   */
  public String getQuery() {
    return query;
  }

  /**
   * @return the facetQuery
   */
  public String getFacetQuery() {
    return facetQuery;
  }

}
