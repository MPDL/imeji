package de.mpg.imeji.logic.generic;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;

/**
 * Abatract class for imeji services with search methods
 *
 * @author saquet
 *
 */
public abstract class SearchServiceAbstract<T> extends ImejiServiceAbstract {
  protected final Search search;

  public SearchServiceAbstract(SearchObjectTypes type) {
    super();
    search = SearchFactory.create(type, SEARCH_IMPLEMENTATIONS.ELASTIC);
  }

  /**
   * Search for Elements
   *
   * @param searchQuery
   * @param sortCri
   * @param user
   * @param size
   * @param offset
   * @return
   */
  public abstract SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user,
      int size, int offset);

  /**
   * Retrieve the search results
   *
   * @param result
   * @return
   * @throws ImejiException
   */
  public abstract List<T> retrieve(List<String> ids, User user) throws ImejiException;

  /**
   * Search and retrieve the search Result
   *
   * @param searchQuery
   * @param sortCri
   * @param user
   * @param size
   * @param offset
   * @return
   * @throws ImejiException
   */
  public List<T> searchAndRetrieve(SearchQuery searchQuery, SortCriterion sortCri, User user,
      int size, int offset) throws ImejiException {
    final SearchResult result = search(searchQuery, sortCri, user, size, offset);
    return retrieve(result.getResults(), user);
  }

  /**
   * Retrieve all elements of type T. This method should be abble to work without search, in order
   * to be used by the reindex process
   *
   * @return
   * @throws ImejiException
   */
  public abstract List<T> retrieveAll() throws ImejiException;

}