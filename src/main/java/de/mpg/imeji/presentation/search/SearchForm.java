/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.search;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.beans.MetadataLabels;

/**
 * The form for the Advanced search. Is composed of {@link SearchGroupForm}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchForm {
  private static final Logger LOGGER = Logger.getLogger(SearchForm.class);
  private Map<String, MetadataProfile> profilesMap;
  private List<SearchGroupForm> groups;
  private SearchPair fileTypeSearch =
      new SearchPair(SearchFields.filetype, SearchOperators.REGEX, "", false);
  private SearchPair allSearch = new SearchPair(SearchFields.all, SearchOperators.REGEX, "", false);
  private boolean includeFulltext = true;


  /**
   * Default Constructor
   */
  public SearchForm() {
    groups = new ArrayList<SearchGroupForm>();
    profilesMap = new HashMap<String, MetadataProfile>();
  }

  /**
   * Constructor for a {@link SearchQuery}: initialize the form from a query
   *
   * @param searchQuery
   * @param collectionsMap
   * @param profilesMap
   * @throws ImejiException
   */
  public SearchForm(SearchQuery searchQuery, Map<String, MetadataProfile> profilesMap,
      MetadataLabels metadataLabels) throws ImejiException {
    this();
    this.profilesMap = profilesMap;
    for (SearchElement se : searchQuery.getElements()) {
      if (se.getType().equals(SEARCH_ELEMENTS.GROUP)) {
        String profileId =
            SearchFormularHelper.getProfileIdFromStatement((SearchGroup) se, profilesMap.values());
        if (profileId != null) {
          groups.add(
              new SearchGroupForm((SearchGroup) se, profilesMap.get(profileId), metadataLabels));
        }
      }
      if (se.getType().equals(SEARCH_ELEMENTS.PAIR)) {
        if (((SearchPair) se).getField() == SearchFields.filetype) {
          fileTypeSearch = new SearchPair(SearchFields.filetype, SearchOperators.REGEX,
              ((SearchPair) se).getValue(), false);
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
        for (SearchElement gse : ((SearchGroup) se).getElements()) {
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
    Set<String> messages = new HashSet<>();
    for (SearchGroupForm g : groups) {
      try {
        g.validate();
      } catch (UnprocessableError e) {
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
      SearchQuery searchQuery = new SearchQuery();
      if (!allSearch.isEmpty()) {
        if (includeFulltext) {
          SearchGroup g = new SearchGroup();
          g.addPair(allSearch);
          g.addLogicalRelation(LOGICAL_RELATIONS.OR);
          g.addPair(new SearchPair(SearchFields.fulltext, SearchOperators.REGEX,
              allSearch.getValue(), false));
        }
      }
      searchQuery.addPair(allSearch);
      for (SearchGroupForm g : groups) {
        if (!searchQuery.isEmpty()) {
          searchQuery.addLogicalRelation(LOGICAL_RELATIONS.OR);
        }
        searchQuery.addGroup(g.getAsSearchGroup());
      }
      if (!searchQuery.isEmpty()) {
        searchQuery.addLogicalRelation(LOGICAL_RELATIONS.AND);
      }
      searchQuery.addPair(fileTypeSearch);
      return searchQuery;
    } catch (UnprocessableError e) {
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
    SearchGroupForm fg = new SearchGroupForm();
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
  public void changeSearchGroup(int pos, MetadataLabels metadataLabels) throws ImejiException {
    SearchGroupForm group = groups.get(pos);
    group.getStatementMenu().clear();
    group.setSearchElementForms(new ArrayList<SearchMetadataForm>());
    if (group.getProfileId() != null) {
      MetadataProfile p = profilesMap.get(group.getProfileId());
      group.initStatementsMenu(p, metadataLabels);
      addElement(pos, 0);
    }
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
  public void addElement(int groupPos, int elPos) {
    SearchGroupForm group = groups.get(groupPos);
    if (group.getStatementMenu().size() > 0) {
      SearchMetadataForm fe = new SearchMetadataForm();
      String namespace = (String) group.getStatementMenu().get(0).getValue();
      fe.setNamespace(namespace);
      fe.initStatement(profilesMap.get(group.getProfileId()), namespace);
      fe.initOperatorMenu();
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
  public void changeElement(int groupPos, int elPos, boolean keepValue) {
    SearchGroupForm group = groups.get(groupPos);
    SearchMetadataForm fe = group.getSearchElementForms().get(elPos);
    String profileId = group.getProfileId();
    String namespace = fe.getNamespace();
    fe.initStatement(profilesMap.get(profileId), namespace);
    fe.initOperatorMenu();
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

  public Map<String, MetadataProfile> getProfilesMap() {
    return profilesMap;
  }

  public void setProfilesMap(Map<String, MetadataProfile> profilesMap) {
    this.profilesMap = profilesMap;
  }

  public SearchPair getFileTypeSearch() {
    return fileTypeSearch;
  }

  public void setFileTypeSearch(SearchPair fileTypeSearch) {
    this.fileTypeSearch = fileTypeSearch;
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
}
