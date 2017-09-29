package de.mpg.imeji.presentation.search.advanced;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Java bean for the advanced search page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "AdvancedSearchBean")
@ViewScoped
public class AdvancedSearchBean extends SuperBean {
  private static final long serialVersionUID = -3989020231445922611L;
  private SearchForm formular;
  // Menus
  private List<SelectItem> collectionsMenu;
  private List<SelectItem> operatorsMenu;

  /**
   * True if the query got an error (for instance wrong date format). Then the message is written in
   * red
   */
  private boolean errorQuery = false;
  private static final Logger LOGGER = Logger.getLogger(AdvancedSearchBean.class);

  @PostConstruct
  public void newSearch() {
    try {
      getNewSearch();
    } catch (final ImejiException e) {
      BeanHelper.error("Error initializing page: " + e.getMessage());
      LOGGER.error("Error initializing advanced search", e);
    }
    resetSelectedItems();
  }

  /**
   * Called when the page is called per get request. Read the query in the url and initialize the
   * form with it
   *
   * @return
   * @throws ImejiException
   */
  public String getNewSearch() throws ImejiException {
    formular = new SearchForm(new SearchQuery(), getLocale(), getSessionUser());
    initMenus();
    try {
      final String query = UrlHelper.getParameterValue("q");
      if (!UrlHelper.getParameterBoolean("error")) {
        errorQuery = false;
        initForm(SearchQueryParser.parseStringQuery(query));
      }
    } catch (final Exception e) {
      LOGGER.error("Error initializing advanced search", e);
      BeanHelper.error("Error initializing advanced search");
    }
    return "";
  }

  /**
   * Init the menus of the page
   */
  private void initMenus() {
    operatorsMenu = new ArrayList<SelectItem>();
    operatorsMenu.add(new SelectItem(LOGICAL_RELATIONS.AND,
        Imeji.RESOURCE_BUNDLE.getLabel("and_small", getLocale())));
    operatorsMenu.add(new SelectItem(LOGICAL_RELATIONS.OR,
        Imeji.RESOURCE_BUNDLE.getLabel("or_small", getLocale())));
  }

  /**
   * Initialized the search form with the {@link SearchQuery}
   *
   * @param searchQuery
   * @throws Exception
   */
  public void initForm(SearchQuery searchQuery) throws Exception {
    formular = new SearchForm(searchQuery, getLocale(), getSessionUser());
  }


  /**
   * Reset the Search form with empty values
   *
   * @throws Exception
   */
  public String reset() throws Exception {
    initForm(new SearchQuery());
    return "";
  }

  /**
   * Method called when form is submitted
   *
   * @return
   * @throws IOException
   */
  public String search() throws IOException {
    goToResultPage();
    return "";
  }

  /**
   * Redirect to the search result page
   *
   * @throws IOException
   */
  public void goToResultPage() throws IOException {
    errorQuery = false;
    try {
      formular.validate();
      final String q = SearchQueryParser.transform2UTF8URL(formular.getFormularAsSearchQuery());
      redirect(getNavigation().getBrowseUrl() + "?q=" + q);
    } catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error invalid search form", e);
    }
  }

  /**
   * Return the current {@link SearchQuery} in the form as a user friendly query
   *
   * @return
   */
  public String getSimpleQuery() {
    if (formular == null) {
      return "";
    }
    return SearchQueryParser.searchQuery2PrettyQuery(formular.getFormularAsSearchQuery());
  }


  /**
   * Getter
   *
   * @return
   */
  public SearchForm getFormular() {
    return formular;
  }

  /**
   * stter
   *
   * @param formular
   */
  public void setFormular(SearchForm formular) {
    this.formular = formular;
  }

  /**
   * Getter
   *
   * @return
   */
  public List<SelectItem> getOperatorsMenu() {
    return operatorsMenu;
  }

  /**
   * Setter
   *
   * @param operatorsMenu
   */
  public void setOperatorsMenu(List<SelectItem> operatorsMenu) {
    this.operatorsMenu = operatorsMenu;
  }

  /**
   * @return the errorQuery
   */
  public boolean getErrorQuery() {
    return errorQuery;
  }

  /**
   * @param errorQuery the errorQuery to set
   */
  public void setErrorQuery(boolean errorQuery) {
    this.errorQuery = errorQuery;
  }


  public List<SelectItem> getCollectionsMenu() {
    return collectionsMenu;
  }

  public void setCollectionsMenu(List<SelectItem> collectionsMenu) {
    this.collectionsMenu = collectionsMenu;
  }

}
