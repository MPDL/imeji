package de.mpg.imeji.presentation.beans;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchIndexes;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.presentation.image.ThumbnailBean;
import de.mpg.imeji.presentation.util.ListUtils;

/**
 * the Java Bean for the Start Page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "StartPageBean")
@ViewScoped
public class StartPageBean extends SuperBean implements Serializable {
  private static Logger LOGGER = Logger.getLogger(StartPageBean.class);
  private static final long serialVersionUID = 5267521759370584976L;
  private List<ThumbnailBean> carousselImages = new ArrayList<ThumbnailBean>();
  private static final int CAROUSSEL_SIZE = 6;
  private Space currentSpace;
  // in hours
  private int searchforItemCreatedForLessThan = 0;
  private boolean carouselEnabled = Imeji.CONFIG.getStartPageCarouselEnabled();


  @PostConstruct
  public void init() {
    if (Imeji.CONFIG.getStartPageCarouselEnabled()) {
      try {
        SearchQuery query = readSearchQueryInProperty();
        SortCriterion order = readSortCriterionInProperty();
        SearchResult result = searchItems(query, order);
        loadItemInCaroussel(result, order == null);
        this.currentSpace = readSpace();
      } catch (Exception e) {
        LOGGER.error("Error initializing start page", e);
      }
    }
  }

  /**
   * Read the search query defined in the imeji.properties
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  private SearchQuery readSearchQueryInProperty() throws ImejiException, URISyntaxException {
    String prop = Imeji.CONFIG.getStartPageCarouselQuery();
    if (prop != null) {
      return SearchQueryParser.parseStringQuery(prop);
    }
    return SearchQueryParser.parseStringQuery("");
  }

  /**
   * Read the order defined in the imeji.properties
   *
   * @return
   * @throws URISyntaxException
   * @throws IOException
   */
  private SortCriterion readSortCriterionInProperty() throws IOException, URISyntaxException {
    try {
      String[] prop = Imeji.CONFIG.getStartPageCarouselQueryOrder().split("-");
      if ("".equals(prop[0]) && "".equals(prop[1])) {
        return new SortCriterion(SearchIndexes.getIndex(prop[0]),
            SortOrder.valueOf(prop[1].toUpperCase()));
      }
    } catch (Exception e) {
      // no sort order defined
    }
    return null;
  }

  /**
   * Search the item for the caroussel
   *
   * @param sq
   * @param sc
   * @return
   */
  private SearchResult searchItems(SearchQuery sq, SortCriterion sc) {
    ItemBusinessController ic = new ItemBusinessController();
    if (sq.isEmpty() && searchforItemCreatedForLessThan > 0) {
      // Search for item which have been for less than n hours
      try {
        sq.addPair(new SearchPair(SearchFields.created, SearchOperators.GREATER,
            getTimeforNDaybeforeNow(searchforItemCreatedForLessThan), false));
      } catch (UnprocessableError e) {
        LOGGER.error("Error building query to search items", e);
      }
      return new SearchResult(
          ic.search(null, sq, sc, getSessionUser(), getSpace(), -1, 0).getResults(), null);
    }
    return ic.search(null, sq, sc, getSessionUser(), getSpace(), -1, 0);
  }

  /**
   * Read the current space
   *
   * @return
   */
  private Space readSpace() {
    if (StringHelper.isNullOrEmptyTrim(getSpace())) {
      return new Space();
    }
    SpaceController sc = new SpaceController();
    try {
      return sc.retrieve(URI.create(getSpace()), getSessionUser());
    } catch (ImejiException e) {
      Space scc = new Space();
      scc.setTitle("Space Not Found");
      scc.setDescription("Space Not Found");
      return scc;
    }
  }

  /**
   * Return the time of the nth day before the current time
   *
   * @param day
   * @return
   */
  private String getTimeforNDaybeforeNow(int n) {
    Calendar cal = Calendar.getInstance();
    cal.add(Calendar.HOUR, -n);
    return DateFormatter.formatToSparqlDateTime(cal);
  }

  /**
   * Load the item the {@link SearchResult} in the caroussel. If random true, will load some random
   * items
   *
   * @param sr
   * @param random
   * @throws ImejiException
   */
  private void loadItemInCaroussel(SearchResult sr, boolean random) throws ImejiException {
    if (sr == null) {
      return;
    }
    ItemBusinessController ic = new ItemBusinessController();
    List<String> uris = new ArrayList<String>();
    if (random) {
      uris = getRandomResults(sr);
    } else {
      int sublistSize = CAROUSSEL_SIZE;
      if (sr.getResults().size() < CAROUSSEL_SIZE) {
        sublistSize = sr.getResults().size();
      }
      if (sublistSize > 0) {
        uris = sr.getResults().subList(0, sublistSize);
      }
    }
    List<Item> items = (List<Item>) ic.retrieveBatchLazy(uris, -1, 0, getSessionUser());
    carousselImages = ListUtils.itemListToThumbList(items);
  }

  /**
   * Takes a number ({@link StartPageBean}.CAROUSSEL_SIZE) of results from a {@link SearchResult}
   *
   * @param sr
   * @return
   */
  private List<String> getRandomResults(SearchResult sr) {
    List<String> l = new ArrayList<String>();
    Random r = new Random();
    while (l.size() < CAROUSSEL_SIZE && l.size() < sr.getNumberOfRecords()) {
      if (sr.getNumberOfRecords() > 0) {
        String uri = sr.getResults().get(r.nextInt(sr.getNumberOfRecords()));
        if (!l.contains(uri)) {
          l.add(uri);
        }
      }
    }
    return l;
  }

  /**
   * setter
   *
   * @param carousselImages
   */
  public void setCarousselImages(List<ThumbnailBean> carousselImages) {
    this.carousselImages = carousselImages;
  }

  /**
   * getter
   *
   * @return
   */
  public List<ThumbnailBean> getCarousselImages() {
    return carousselImages;
  }

  public Space getSelectedSpaceResource() {
    return currentSpace;
  }

  public boolean isCarouselEnabled() {
    return carouselEnabled;
  }

  public void setCarouselEnabled(boolean carouselEnabled) {
    this.carouselEnabled = carouselEnabled;
  }

}
