package de.mpg.imeji.presentation.search.breadcrumb;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.facet.FacetService;
import de.mpg.imeji.logic.facet.model.Facet;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

@ManagedBean(name = "SearchBreadcrumb")
@ViewScoped
public class SearchBreadcrumbBean extends SuperBean {
  private static final long serialVersionUID = -5353804541950059766L;
  private static final Logger LOGGER = Logger.getLogger(SearchBreadcrumbBean.class);
  private SearchQuery facetQuery = new SearchQuery();
  private FacetService facetService = new FacetService();
  private List<SearchBreadcrumbEntry> entries = new ArrayList<>();


  @PostConstruct
  public void init() {
    String facetQueryString = UrlHelper.getParameterValue("fq");
    try {
      facetQuery = SearchQueryParser.parseStringQuery(facetQueryString);
    } catch (UnprocessableError e) {
      BeanHelper.error("Error reading facet Query in the url");
      LOGGER.error("Error reading facet Query in the url", e);
    }
    entries = initEntries(facetQuery.getElements());
  }

  private List<SearchBreadcrumbEntry> initEntries(List<SearchElement> elements) {
    List<SearchBreadcrumbEntry> l = new ArrayList<>();
    for (SearchElement el : elements) {
      if (el instanceof SearchMetadata) {
        Facet f = facetService
            .retrieveByIndexFromCache(SearchQueryParser.getMetadataIndex((SearchMetadata) el));
        if (f != null) {
          l.add(new SearchBreadcrumbEntry(f, ((SearchMetadata) el).getValue(),
              getRemoveQuery(facetQuery, el)));
        }
      } else if (el instanceof SearchPair) {
        // TODO
      } else if (el instanceof SearchGroup) {
        l.addAll(initEntries(el.getElements()));
      }
    }
    return l;
  }

  /**
   * Return the query to remove this {@link SearchElement} from the {@link SearchQuery}
   * 
   * @param q
   * @param el
   * @return
   */
  private String getRemoveQuery(SearchQuery q, SearchElement el) {
    List<SearchElement> elements = new ArrayList<>();
    for (SearchElement element : q.getElements()) {
      if (!element.isSame(el)) {
        elements.add(element);
      } else if (element.isSame(el) && elements.size() > 0) {
        elements.remove(elements.size() - 1);
      }
    }
    return SearchQueryParser.transform2UTF8URL(new SearchQuery(elements));
  }

  public String getAddFacetQuery(String index, String value) throws UnprocessableError {
    SearchQuery q = new SearchFactory(facetQuery)
        .addElement(new SearchMetadata(index, value), LOGICAL_RELATIONS.AND).build();
    return SearchQueryParser.transform2UTF8URL(q);
  }

  /**
   * @return the entries
   */
  public List<SearchBreadcrumbEntry> getEntries() {
    return entries;
  }

  /**
   * @param entries the entries to set
   */
  public void setEntries(List<SearchBreadcrumbEntry> entries) {
    this.entries = entries;
  }


}
