/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.item;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
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
  private static final Logger LOGGER = Logger.getLogger(ItemDetailsBrowse.class);
  private String q;
  private String containerUri;
  private int currentPosition;
  private SortCriterion sortCriterion;
  private static final int SIZE = 3;
  private Item currentItem = null;
  private String next = null;
  private String previous = null;

  /**
   * Object for the browsing over the detail items. The Browsing is based on a {@link ItemsBean} and
   * the current {@link Item}.
   *
   * @param imagesBean
   * @param item
   * @param type
   * @param containerId
   */
  public ItemDetailsBrowse(Item item, String type, String containerUri, User user, String spaceId) {
    this.q = UrlHelper.hasParameter("q") ? UrlHelper.getParameterValue("q") : "";
    this.containerUri = containerUri;
    this.currentItem = item;
    this.sortCriterion = initSortCriterion();
    this.currentPosition =
        UrlHelper.hasParameter("pos") ? Integer.parseInt(UrlHelper.getParameterValue("pos")) : -1;
    List<String> items = searchPreviousAndNextItem(user, spaceId);
    init(items, type, containerUri);
  }

  /**
   * Search for items previous and after to the current item.
   * 
   * @param user
   * @param spaceId
   * @return
   */
  private List<String> searchPreviousAndNextItem(User user, String spaceId) {
    ItemBusinessController controller = new ItemBusinessController();
    try {
      return controller.search(containerUri != null ? URI.create(containerUri) : null,
          SearchQueryParser.parseStringQuery(q), sortCriterion, user, spaceId, SIZE, getOffset())
          .getResults();
    } catch (UnprocessableError e) {
      LOGGER.error("Error retrieving items", e);
    }
    return new ArrayList<>();
  }

  /**
   * Initialize the Sort Criterion
   * 
   * @return
   */
  private SortCriterion initSortCriterion() {
    String sortFieldName =
        CookieUtils.readNonNull(ItemsBean.ITEM_SORT_COOKIE, SearchFields.modified.name());
    String orderFieldName =
        CookieUtils.readNonNull(ItemsBean.ITEM_SORT_ORDER_COOKIE, SortOrder.DESCENDING.name());
    return new SortCriterion(SearchFields.valueOf(sortFieldName),
        SortOrder.valueOf(orderFieldName));
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
   */
  public void init(List<String> items, String type, String containerUri) {
    String baseUrl = new String();
    if (type == "collection") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getCollectionUrl()
          + ObjectHelper.getId(URI.create(containerUri)) + "/item/";
    } else if (type == "item") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getItemUrl();
    } else if (type == "album") {
      baseUrl = ((Navigation) BeanHelper.getApplicationBean(Navigation.class)).getAlbumUrl()
          + ObjectHelper.getId(URI.create(containerUri)) + "/item/";
    }
    String nextItem = nextItem(items);
    String previousItem = previousItem(items);
    if (nextItem != null) {
      next = baseUrl + ObjectHelper.getId(URI.create(nextItem)) + "?q=" + this.q + "&pos="
          + (this.currentPosition + 1);
    }
    if (previousItem != null) {
      previous = baseUrl + ObjectHelper.getId(URI.create(previousItem)) + "?q=" + this.q + "&pos="
          + (this.currentPosition - 1);
    }
  }

  /**
   * Return the item id next to the current item
   * 
   * @param items
   * @return
   */
  private String nextItem(List<String> items) {
    int currentIndex = items.indexOf(currentItem.getId().toString());
    return currentIndex < items.size() - 1 ? items.get(currentIndex + 1) : null;
  }

  /**
   * Return the item id previous to the current item
   * 
   * @param items
   * @return
   */
  private String previousItem(List<String> items) {
    int currentIndex = items.indexOf(currentItem.getId().toString());
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
}
