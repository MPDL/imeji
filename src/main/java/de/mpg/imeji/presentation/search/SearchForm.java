/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.vo.User;

/**
 * The form for the Advanced search. Is composed of {@link SearchGroupForm}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchForm implements Serializable {
  private static final long serialVersionUID = 9203984025130411565L;
  private static final Logger LOGGER = Logger.getLogger(SearchForm.class);
  private List<SearchGroupForm> groups;
  private LicenseSearchGroup licenseSearchGroup;
  private FileTypeSearchGroup fileTypeSearchGroup;
  private TechnicalMetadataSearchGroup technicalMetadataSearchGroup;
  private SearchPair allSearch = new SearchPair(SearchFields.all, SearchOperators.REGEX, "", false);
  private boolean includeFulltext = true;


  /**
   * Default Constructor
   */
  public SearchForm() {
    groups = new ArrayList<SearchGroupForm>();
  }

  /**
   * Constructor for a {@link SearchQuery}: initialize the form from a query
   *
   * @param searchQuery
   * @param collectionsMap
   * @param profilesMap
   * @throws ImejiException
   */
  public SearchForm(SearchQuery searchQuery, Locale locale, User user) throws ImejiException {
    this();
    this.licenseSearchGroup = new LicenseSearchGroup(locale);
    this.fileTypeSearchGroup = new FileTypeSearchGroup(locale);
    this.setTechnicalMetadataSearchGroup(new TechnicalMetadataSearchGroup());
    for (final SearchElement se : searchQuery.getElements()) {
      if (se.getType().equals(SEARCH_ELEMENTS.GROUP)) {

      }
      if (se.getType().equals(SEARCH_ELEMENTS.PAIR)) {
        if (((SearchPair) se).getField() == SearchFields.filetype) {
          fileTypeSearchGroup = new FileTypeSearchGroup(((SearchPair) se).getValue(), locale);
        }
        if (((SearchPair) se).getField() == SearchFields.license) {
          licenseSearchGroup = new LicenseSearchGroup(((SearchPair) se).getValue(), locale);
        }
      }
      parseAllFieldSearch(se);
    }
  }


  /**
   * Find the search for all field
   *
   * @param se
   */
  private void parseAllFieldSearch(SearchElement se) {
    if (allSearch.getValue().isEmpty()) {
      if (se.getType().equals(SEARCH_ELEMENTS.PAIR)
          && ((SearchPair) se).getField() == SearchFields.all) {
        setAllSearch(new SearchPair(SearchFields.all, SearchOperators.REGEX,
            ((SearchPair) se).getValue(), false));
      } else if (se.getType().equals(SEARCH_ELEMENTS.GROUP)) {
        for (final SearchElement gse : ((SearchGroup) se).getElements()) {
          parseAllFieldSearch(gse);
        }
      }
    }
  }

  /**
   * Validate the Search form according the user input
   *
   * @throws UnprocessableError
   */
  public void validate() throws UnprocessableError {
    final Set<String> messages = new HashSet<>();
    for (final SearchGroupForm g : groups) {
      try {
        g.validate();
      } catch (final UnprocessableError e) {
        messages.addAll(e.getMessages());
      }
    }
    if (!messages.isEmpty()) {
      throw new UnprocessableError(messages);
    }
    if ("".equals(SearchQueryParser.transform2UTF8URL(getFormularAsSearchQuery()))) {
      throw new UnprocessableError("error_search_query_emtpy");
    }
  }

  /**
   * Transform the {@link SearchForm} in a {@link SearchQuery}
   *
   * @return
   */
  public SearchQuery getFormularAsSearchQuery() {
    try {
      final SearchQuery searchQuery = new SearchQuery();
      if (!allSearch.getValue().isEmpty()) {
        if (includeFulltext) {
          final SearchGroup g = new SearchGroup();
          g.addPair(allSearch);
          g.addLogicalRelation(LOGICAL_RELATIONS.OR);
          g.addPair(new SearchPair(SearchFields.fulltext, SearchOperators.REGEX,
              allSearch.getValue(), false));
          searchQuery.addGroup(g);
        } else {
          searchQuery.addPair(allSearch);
        }
      }
      final SearchGroup metadataGroup = new SearchGroup();
      for (final SearchGroupForm g : groups) {
        final SearchGroup mdGroup = g.getAsSearchGroup();
        if (!mdGroup.isEmpty()) {
          if (!metadataGroup.isEmpty()) {
            metadataGroup.addLogicalRelation(LOGICAL_RELATIONS.OR);
          }
          metadataGroup.addGroup(g.getAsSearchGroup());
        }
      }
      if (!metadataGroup.isEmpty()) {
        if (!searchQuery.isEmpty()) {
          searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
        }
        searchQuery.addGroup(metadataGroup);
      }

      if (!fileTypeSearchGroup.isEmpty()) {
        if (!searchQuery.isEmpty()) {
          searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
        }
        searchQuery.addPair(fileTypeSearchGroup.getPair());
      }

      if (!licenseSearchGroup.isEmpty()) {
        if (!searchQuery.isEmpty()) {
          searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
        }
        searchQuery.addPair(licenseSearchGroup.asSearchPair());
      }

      if (!technicalMetadataSearchGroup.isEmpty()) {
        if (!searchQuery.isEmpty()) {
          searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
        }
        searchQuery.addGroup(technicalMetadataSearchGroup.asSearchGroup());
      }

      return searchQuery;
    } catch (final UnprocessableError e) {
      LOGGER.error("Error transforming search form to searchquery", e);
      return new SearchQuery();
    }
  }

  /**
   * Add a {@link SearchGroup} to the form
   *
   * @param pos
   */
  public void addSearchGroup(int pos) {
    final SearchGroupForm fg = new SearchGroupForm();
    if (pos >= groups.size()) {
      groups.add(fg);
    } else {
      groups.add(pos + 1, fg);
    }
  }

  /**
   * Method called when the selected collection is changed in the select menu
   *
   * @param pos
   * @throws ImejiException
   */
  public void changeSearchGroup(int pos, User user) throws ImejiException {
    final SearchGroupForm group = groups.get(pos);
    group.getStatementMenu().clear();
    group.setSearchElementForms(new ArrayList<SearchMetadataForm>());
  }

  /**
   * Method called when the buttom remove group is called
   *
   * @param pos
   */
  public void removeSearchGroup(int pos) {
    groups.remove(pos);
  }

  /**
   * Method called when the button add element is called
   *
   * @param groupPos
   * @param elPos
   */
  public void addElement(int groupPos, int elPos, Locale locale) {
    final SearchGroupForm group = groups.get(groupPos);
    if (group.getStatementMenu().size() > 0) {
      final SearchMetadataForm fe = new SearchMetadataForm();
      final String namespace = (String) group.getStatementMenu().get(0).getValue();
      fe.setNamespace(namespace);
      fe.initOperatorMenu(locale);
      if (elPos >= group.getSearchElementForms().size()) {
        group.getSearchElementForms().add(fe);
      } else {
        group.getSearchElementForms().add(elPos + 1, fe);
      }
    }
  }

  /**
   * Change the statement type of the element
   *
   * @param groupPos
   * @param elPos
   */
  public void changeElement(int groupPos, int elPos, boolean keepValue, Locale locale) {
    final SearchGroupForm group = groups.get(groupPos);
    final SearchMetadataForm fe = group.getSearchElementForms().get(elPos);
    final String profileId = group.getProfileId();
    final String namespace = fe.getNamespace();
    fe.initOperatorMenu(locale);
    if (!keepValue) {
      fe.setSearchValue("");
    }
  }

  public void removeElement(int groupPos, int elPos) {
    groups.get(groupPos).getSearchElementForms().remove(elPos);
  }

  public List<SearchGroupForm> getGroups() {
    return groups;
  }

  public void setGroups(List<SearchGroupForm> groups) {
    this.groups = groups;
  }

  public SearchPair getAllSearch() {
    return allSearch;
  }

  public void setAllSearch(SearchPair allSearch) {
    this.allSearch = allSearch;
  }

  public boolean isIncludeFulltext() {
    return includeFulltext;
  }

  public void setIncludeFulltext(boolean includeFulltext) {
    this.includeFulltext = includeFulltext;
  }

  /**
   * @return the licenseSearchGroup
   */
  public LicenseSearchGroup getLicenseSearchGroup() {
    return licenseSearchGroup;
  }

  /**
   * @param licenseSearchGroup the licenseSearchGroup to set
   */
  public void setLicenseSearchGroup(LicenseSearchGroup licenseSearchGroup) {
    this.licenseSearchGroup = licenseSearchGroup;
  }

  /**
   * @return the fileTypeSearchGroup
   */
  public FileTypeSearchGroup getFileTypeSearchGroup() {
    return fileTypeSearchGroup;
  }

  /**
   * @param fileTypeSearchGroup the fileTypeSearchGroup to set
   */
  public void setFileTypeSearchGroup(FileTypeSearchGroup fileTypeSearchGroup) {
    this.fileTypeSearchGroup = fileTypeSearchGroup;
  }

  /**
   * @return the technicalMetadataSearchGroup
   */
  public TechnicalMetadataSearchGroup getTechnicalMetadataSearchGroup() {
    return technicalMetadataSearchGroup;
  }

  /**
   * @param technicalMetadataSearchGroup the technicalMetadataSearchGroup to set
   */
  public void setTechnicalMetadataSearchGroup(
      TechnicalMetadataSearchGroup technicalMetadataSearchGroup) {
    this.technicalMetadataSearchGroup = technicalMetadataSearchGroup;
  }

}
