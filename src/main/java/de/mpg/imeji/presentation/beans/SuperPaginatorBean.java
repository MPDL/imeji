/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.beans;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.util.CookieUtils;

/**
 * This abstract bean class is used to manage lists with one or two paginators. It can work together
 * with different BaseListRetrieverRequestBeans that are responsible to retrieve the list elements.
 * On a jsp page, at first the BaseListRetrieverRequestBean has to be initialized. It then
 * automatically updates the list in this bean (by calling update) whenever necessary and reads
 * required GET parameters (via readOutParamaters()). This bean has to be managed in the session
 * scope of JSF. The list only refreshes if any GET parameters have changed or new parameters have
 * been added. If you want to refresh the list anyway, please call the hasChanged method.
 *
 * @author Markus Haarlaender (initial creation)
 * @author $Author: haarlaender $ (last modification)
 * @version $Revision: 3185 $ $LastChangedDate: 2009-11-20 14:30:15 +0100 (Fri, 20 Nov 2009) $
 * @param <ListElementType> The Type of the list elements managed by this bean
 * @param <FilterType> The type of filters managed by this bean that are usable for every
 *        ListRetriever, eg. sorting of PubItems.
 */
public abstract class SuperPaginatorBean<ListElementType> extends SuperBean {
  private static final long serialVersionUID = -3493783822585689753L;
  protected static Logger LOGGER = Logger.getLogger(SuperPaginatorBean.class);
  public static final String numberOfItemsPerPageCookieName = "IMEJI_ITEMS_PER_PAGE";
  public static final String numberOfContainersPerPageCookieName = "IMEJI_CONTAINERS_PER_PAGE";
  /**
   * A list that contains the menu entries of the elements per page menu.
   */
  private List<SelectItem> elementsPerPageSelectItems;
  /**
   * A list containing the PaginatorPage objects
   */
  private List<PaginatorPage> paginatorPageList;
  /**
   * The list containing the current elements of the displayed list
   */
  private List<ListElementType> currentPartList;
  /**
   * The current number of elements per page
   */
  private int elementsPerPage = 0;
  /**
   * The current paginator page number
   */
  private int currentPageNumber;
  /**
   * This attribute is bound to the 'go to' input field of the upper paginator
   */
  private String goToPage;
  /**
   * The total number of elements that are in the complete list (without any limit or offset
   * filters). corresponding BaseListRetrieverRequestBean.
   */
  private int totalNumberOfElements = 0;

  private List<SelectItem> sortMenu;
  private String selectedSortCriterion;
  private String selectedSortOrder = SortOrder.DESCENDING.name();

  /**
   * Types of paginators
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public enum PAGINATOR_TYPE {
    ITEMS, COLLECTIONS, ALBUMS, ALBUM_ITEMS, COLLECTION_ITEMS, PRIVATE;
  }

  /**
   * Initializes a new BasePaginatorListSessionBean
   */
  public SuperPaginatorBean() {
    paginatorPageList = new ArrayList<PaginatorPage>();
    currentPartList = new ArrayList<ListElementType>();
    elementsPerPageSelectItems = new ArrayList<SelectItem>();
    readUrlParameters();
  }

  /**
   * Should be called by bean in a postconstruct method
   */
  public void init() {
    initSortMenu();
    initElementsPerPageMenu();
    setCurrentPageNumber(1);
  }

  /**
   * Read if there are navigation parameters in the url
   * 
   * @return
   */
  public void readUrlParameters() {
    if (FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .containsKey("page")) {
      currentPageNumber = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
          .getRequestParameterMap().get("page"));
    }
    if (FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
        .containsKey("el")) {
      setElementsPerPage(Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
          .getRequestParameterMap().get("el")));
    }
  }

  /**
   * Initialize the Sorting menu
   */
  public abstract void initSortMenu();

  /**
   * Initialize the Men for elements per page
   */
  public abstract void initElementsPerPageMenu();

  /**
   * This method is called by the corresponding BaseListRetrieverRequestBean whenever the list has
   * to be updated. It reads out basic parameters and calls readOutParamters on implementing
   * subclasses. It uses the BaseListRetrieverRequestBean in order to retrieve the new list and
   * finally calls listUpdated on implementing subclasses.
   */
  public void update() {
    try {
      if (elementsPerPage == 0) {
        setElementsPerPage(24);
      }
      if (currentPageNumber == 0) {
        setCurrentPageNumber(1);
      }
      setGoToPage(Integer.toString(currentPageNumber));
      currentPartList.clear();
      currentPartList = retrieveList(getOffset(), elementsPerPage);
      totalNumberOfElements = getTotalNumberOfRecords();
      paginatorPageList.clear();
      for (int i = 0; i < ((getTotalNumberOfElements() - 1) / elementsPerPage) + 1; i++) {
        paginatorPageList.add(new PaginatorPage(i + 1));
      }
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error paginator list update ", e);
    }
  }



  /**
   * Returns the current list with the specified elements
   *
   * @return
   */
  public List<ListElementType> getCurrentPartList() {
    return currentPartList;
  }

  /**
   * Returns the size of the current list
   *
   * @return
   */
  public int getPartListSize() {
    return getCurrentPartList().size();
  }

  /**
   * Returns the total number of elements, without offset and limit filters. Drawn from
   * BaseRetrieverRequestBean
   */
  public int getTotalNumberOfElements() {
    return totalNumberOfElements;
  }

  /**
   * Returns the current offset (starting with 0)
   */
  public int getOffset() {
    return ((currentPageNumber - 1) * elementsPerPage);
  }

  /**
   * Return the first page of the paginator
   *
   * @return
   */
  public int getPageOffset() {
    if (getFirstPaginatorPageNumber() > 1) {
      return getFirstPaginatorPageNumber() - 1;
    }
    return 0;
  }

  /**
   * Sets the current value for 'element per pages'
   */
  public void setElementsPerPage(int elementsPerPage) {
    this.elementsPerPage = elementsPerPage;
    setCookieElementPerPage();
  }

  /**
   * Set the cookie for number of elements per page (per default, for items)
   */
  public void setCookieElementPerPage() {
    CookieUtils.updateCookieValue(numberOfItemsPerPageCookieName,
        Integer.toString(elementsPerPage));
  }

  /**
   * Set the cookie with the sort value
   */
  protected abstract void setCookieSortValue(String value);

  /**
   * set the cookie with the sort order
   */
  protected abstract void setCookieSortOrder(String order);

  /**
   * Returns the currently selected number of elements per page
   *
   * @return
   */
  public int getElementsPerPage() {
    return elementsPerPage;
  }

  /**
   * Listener for elementsPerPageTop
   *
   * @param event
   * @throws Exception
   */
  public void elementsPerPageListener(ValueChangeEvent event) throws Exception {
    if (event != null) {
      setElementsPerPage((Integer) event.getNewValue());
      setCurrentPageNumber(1);
      update();
    }
  }

  public void gotoPageNumber(int page) {
    try {
      if (page > 0 && page <= getPaginatorPageSize()) {
        setCurrentPageNumber(page);
      } else {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_page_not_exists", getLocale()));
      }
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_integer_required", getLocale()));
    }
    update();
  }

  /**
   * Returns the current page number of the paginator
   *
   * @return
   */
  public int getCurrentPageNumber() {
    return currentPageNumber;
  }

  /**
   * Returns a list with the paginator pages. Used from jsf to iterate over the numbers
   *
   * @return
   */
  public List<PaginatorPage> getPaginatorPages() {
    return paginatorPageList;
  }

  /**
   * Returns the number of all paginator pages, not only the visible ones
   *
   * @return
   */
  public int getPaginatorPageSize() {
    return getPaginatorPages().size();
  }

  /**
   * Returns the number of the paginator page button that should be displayed as first button of the
   * paginator in order to always display exactly seven paginator page buttons
   *
   * @return
   */
  public int getFirstPaginatorPageNumber() {
    if (getPaginatorPageSize() > 7 && currentPageNumber > getPaginatorPageSize() - 4) {
      return getPaginatorPageSize() - 6;
    } else if (getPaginatorPageSize() > 7 && currentPageNumber > 4) {
      return currentPageNumber - 3;
    } else {
      return 1;
    }
  }

  /**
   * Sets the menu entries of the elements per page menu
   *
   * @param elementsPerPageSelectItems
   */
  public void setElementsPerPageSelectItems(List<SelectItem> elementsPerPageSelectItems) {
    this.elementsPerPageSelectItems = elementsPerPageSelectItems;
  }

  /**
   * Returns the menu entries of the elements per page menu
   *
   * @return
   */
  public List<SelectItem> getElementsPerPageSelectItems() {
    return elementsPerPageSelectItems;
  }

  public List<SelectItem> getSortMenu() {
    return sortMenu;
  }

  public void setSortMenu(List<SelectItem> sortMenu) {

    this.sortMenu = sortMenu;
  }

  public String getSelectedSortCriterion() {
    return selectedSortCriterion;
  }

  public void setSelectedSortCriterion(String selectedSortCriterion) {
    this.selectedSortCriterion = selectedSortCriterion;
  }

  public void changeSelectedSortCriterion(String selectedSortCriterion) {
    if (!this.selectedSortCriterion.equals(selectedSortCriterion)) {
      this.selectedSortCriterion = selectedSortCriterion;
      setCookieSortValue(this.selectedSortCriterion);
      update();
    } else {
      toggleSortOrder();
    }
  }

  public String getSelectedSortOrder() {
    return selectedSortOrder;
  }

  public void setSelectedSortOrder(String selectedSortOrder) {
    this.selectedSortOrder = selectedSortOrder;
  }

  public void toggleSortOrder() {
    if (selectedSortOrder.equals("DESCENDING")) {
      selectedSortOrder = "ASCENDING";
    } else {
      selectedSortOrder = "DESCENDING";
    }
    update();
    setCookieSortOrder(selectedSortOrder);
  }



  /**
   * Inner class pf which an instance represents an paginator button. Used by the iterator in jsf.
   *
   * @author Markus Haarlaender (initial creation)
   * @author $Author: haarlaender $ (last modification)
   * @version $Revision: 3185 $ $LastChangedDate: 2009-11-20 14:30:15 +0100 (Fri, 20 Nov 2009) $
   */
  public class PaginatorPage {
    /**
     * The page number of the paginator button
     */
    private int number;

    public PaginatorPage(int number) {
      this.number = number;
    }

    /**
     * Sets the page number of the paginator button
     *
     * @param number
     */
    public void setNumber(int number) {
      this.number = number;
    }

    /**
     * Returns the page number of the paginator button
     *
     * @return
     */
    public int getNumber() {
      return number;
    }

    /**
     * Returns the link that is used as output link of the paginator page button
     *
     * @return
     */
    public String goToPage() {
      setCurrentPageNumber(getNumber());
      return "";
    }
  }

  /**
   * Returns the link for the "Next"-Button of the Paginator
   *
   * @return
   */
  public void goToNextPage() {
    currentPageNumber += 1;
    update();
  }

  /**
   * Returns the link for the "Previous"-Button of the Paginator
   *
   * @return
   */
  public void goToPreviousPage() {
    currentPageNumber -= 1;
    update();
  }

  /**
   * Returns the link for the "First Page"-Button of the Paginator
   *
   * @return
   */
  public void goToFirstPage() {
    currentPageNumber = 1;
    update();
  }

  /**
   * Returns the link for the "Last Page"-Button of the Paginator
   *
   * @return
   */
  public void goToLastPage() {
    currentPageNumber = getPaginatorPageSize();
    update();
  }

  /**
   * Returns the link for the "Last Page"-Button of the Paginator
   *
   * @return
   */
  public void goToPage() {
    currentPageNumber = Integer.parseInt(getGoToPage());
    update();
  }

  /**
   * Sets the current paginator page number
   *
   * @param currentPageNumber
   */
  public void setCurrentPageNumber(int currentPageNumber) {
    this.currentPageNumber = currentPageNumber;
  }

  /**
   * WARNING: USE THIS METHOD ONLY FROM UPPER GO TO INPUT FIELD MENU IN JSPF. For setting the value
   * manually, use setGoToPage().
   *
   * @param elementsPerPageTop
   */
  public void setGoToPage(String goToPage) {
    this.goToPage = goToPage;
  }

  /**
   * WARNING: USE THIS METHOD ONLY FROM UPPER GO TO INPUT FIELD MENU IN JSPF. For getting the value
   * manually, use getGoToPage().
   *
   * @param elementsPerPageTop
   */
  public String getGoToPage() {
    return this.goToPage;
  }



  /**
   * Whenever this method is called, an updated list with elements of type ListelementType has to be
   * returned
   *
   * @param offset An offset from where the list must start (0 means at the beginning of all
   *        records)
   * @param limit The length of the list that has to be returned. If the whole list has less records
   *        than this paramter allows, a smaller list can be returned.
   * @param additionalFilters Additional filters that have to be included when retrieving the list.
   * @return
   * @throws Exception
   */
  public abstract List<ListElementType> retrieveList(int offset, int size) throws Exception;

  /**
   * Must return the total size of the retrieved list without limit and offset parameters. E.g. for
   * a search the whole number of search records
   *
   * @return The whole number of elements in the list, regardless of limit and offset parameters
   */
  public abstract int getTotalNumberOfRecords();

  public abstract String getNavigationString();



  /**
   * return the {@link PAGINATOR_TYPE} of the current bean
   *
   * @return
   */
  public abstract String getType();
}
