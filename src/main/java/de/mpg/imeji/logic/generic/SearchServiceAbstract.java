package de.mpg.imeji.logic.generic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
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
  private final ExecutorService executor = Executors.newCachedThreadPool();

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
   * Retrieve the items as Future
   * 
   * @param ids
   * @param user
   * @return
   */
  public Future<List<T>> retrieveAsync(List<String> ids, User user) {
    return executor.submit(new RetrieveJob(ids, user));
  }

  /**
   * Retrieve all elements of type T. This method should be abble to work without search, in order
   * to be used by the reindex process
   *
   * @return
   * @throws ImejiException
   */
  public List<T> retrieveAll() throws ImejiException {
    return retrieve(searchAll(), Imeji.adminUser);
  };

  /**
   * Search for all elements of type T and return the list of uri
   * 
   * @return
   */
  public abstract List<String> searchAll();

  /**
   * Iterate over all elements of Type T. Should be prefer to retreiveAll to avoid to create huge
   * list of objects
   * 
   * @param stepSize
   * @return
   */
  public RetrieveIterator iterateAll(int stepSize) {
    return new RetrieveIterator(searchAll(), Imeji.adminUser, stepSize);
  }


  /**
   * Iterate over a search result
   * 
   * @param result
   * @param user
   * @param stepSize
   * @return
   */
  public RetrieveIterator iterate(SearchResult result, User user, int stepSize) {
    return new RetrieveIterator(result.getResults(), Imeji.adminUser, stepSize);
  }

  /**
   * Interate over a list
   * 
   * @author saquet
   *
   */
  public class RetrieveIterator implements Iterator<List<T>> {
    private final Iterator<String> uriIterator;
    private final User user;
    private final int stepSize;
    private final int size;

    public RetrieveIterator(List<String> uris, User user, int stepSize) {
      this.uriIterator = uris.iterator();
      this.user = user;
      this.stepSize = stepSize;
      this.size = uris.size();
    }

    public int getSize() {
      return size;
    }

    @Override
    public boolean hasNext() {
      return uriIterator.hasNext();
    }

    @Override
    public List<T> next() {
      try {
        return retrieve(next(stepSize), user);
      } catch (ImejiException e) {
        return new ArrayList<>();
      }
    }

    /**
     * Get next nth uris
     * 
     * @param stepSize
     * @return
     */
    private List<String> next(int stepSize) {
      List<String> uris = new ArrayList<>(stepSize);
      while (uriIterator.hasNext() && uris.size() < stepSize) {
        final String uri = (String) uriIterator.next();
        uris.add(uri);
      }
      return uris;
    }
  }

  public static void main(String[] args) {
    List<String> uris = new ArrayList<>(120);
    System.out.println(uris.size());
  }

  /**
   * Job to retrieve the objects and return a Future
   * 
   * @author saquet
   *
   */
  private class RetrieveJob implements Callable<List<T>> {
    private final List<String> ids;
    private final User user;

    public RetrieveJob(List<String> ids, User user) {
      this.ids = ids;
      this.user = user;
    }

    @Override
    public List<T> call() throws Exception {
      return retrieve(ids, user);
    }
  }

}
