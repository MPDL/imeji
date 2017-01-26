package de.mpg.imeji.presentation.search;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchQuery;
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
  private SearchForm formular = new SearchForm();
  // Menus
  private List<SelectItem> profilesMenu;
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
    if (formular.getGroups().size() == 0) {
      formular.addSearchGroup(0);
    }
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
   * Change the {@link SearchGroup}
   *
   * @throws ImejiException
   */
  public void changeGroup() throws ImejiException {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.changeSearchGroup(gPos, getSessionUser());
  }

  /**
   * Add a new {@link SearchGroupForm}
   */
  public void addGroup() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.addSearchGroup(gPos);
  }

  /**
   * Remove a {@link SearchGroupForm}
   */
  public void removeGroup() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    formular.removeSearchGroup(gPos);
    if (formular.getGroups().size() == 0) {
      formular.addSearchGroup(0);
    }
  }

  /**
   * Change a {@link SearchMetadataForm}. The search value is removed
   */
  public void changeElement() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    final int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.changeElement(gPos, elPos, false, getLocale());
  }

  /**
   * Update a {@link SearchMetadataForm}. The search value is keeped
   */
  public void updateElement() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    final int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.changeElement(gPos, elPos, true, getLocale());
  }

  public void updateForm() {

  }

  /**
   * Add a new {@link SearchMetadataForm}
   */
  public void addElement() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    final int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.addElement(gPos, elPos, getLocale());
  }

  /**
   * Remove a new {@link SearchMetadataForm}
   */
  public void removeElement() {
    final int gPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("gPos"));
    final int elPos = Integer.parseInt(FacesContext.getCurrentInstance().getExternalContext()
        .getRequestParameterMap().get("elPos"));
    formular.removeElement(gPos, elPos);
    if (formular.getGroups().get(gPos).getSearchElementForms().size() == 0) {
      formular.removeSearchGroup(gPos);
      formular.addSearchGroup(gPos);
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
    return SearchQueryParser.searchQuery2PrettyQuery(formular.getFormularAsSearchQuery(),
        getLocale());
  }

  /**
   * Getter
   *
   * @return
   */
  public List<SelectItem> getProfilesMenu() {
    return profilesMenu;
  }

  /**
   * Setter
   *
   * @param collectionsMenu
   */
  public void setProfilesMenu(List<SelectItem> profilesMenu) {
    this.profilesMenu = profilesMenu;
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
