/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Callable to exectute Facets objects
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class FacetsJob implements Callable<Boolean> {
  private List<List<Facet>> facets = new ArrayList<List<Facet>>();
  private boolean running = false;
  private FacetsAbstract facetsClass;
  private static final Logger LOGGER = Logger.getLogger(FacetsJob.class);

  /*
   * (non-Javadoc)
   *
   * @see java.util.concurrent.Callable#call()
   */
  @Override
  public Boolean call() {
    try {
      running = true;
      facetsClass.init();
      facets = facetsClass.getFacets();
    } catch (Exception e) {
      LOGGER.error("Error initializing facets", e);
    } finally {
      running = false;
    }
    return running;
  }

  /**
   * Initialize the {@link FacetsJob} for one {@link SearchQuery} from the collection browse page
   *
   * @param col
   * @param searchQuery
   */
  public FacetsJob(CollectionImeji col, SearchQuery searchQuery, SearchResult searchRes, User user,
      Locale locale) {
    try {
      facetsClass = new CollectionFacets(col, searchQuery, searchRes, user, locale);
    } catch (Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("error", BeanHelper.getLocale())
          + ", Collection Facets intialization : " + e.getMessage());
    }
  }

  /**
   * getter
   *
   * @return
   */
  public List<List<Facet>> getFacets() {
    return facets;
  }

  /**
   * setter
   *
   * @param facets
   */
  public void setFacets(List<List<Facet>> facets) {
    this.facets = facets;
  }


  /**
   * @return the running
   */
  public boolean isRunning() {
    return running;
  }
}
