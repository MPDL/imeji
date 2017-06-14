package de.mpg.imeji.presentation.search;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.facet.FacetService;
import de.mpg.imeji.logic.facet.model.Facet;
import de.mpg.imeji.logic.facet.model.FacetResults;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
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
  private List<FacetResults> facets = new ArrayList<>();
  private FacetService facetService = new FacetService();


  @PostConstruct
  public void init() {
    String facetQueryString = UrlHelper.getParameterValue("fq");
    try {
      facetQuery = SearchQueryParser.parseStringQuery(facetQueryString);
    } catch (UnprocessableError e) {
      BeanHelper.error("Error reading facet Query in the url");
      LOGGER.error("Error reading facet Query in the url", e);
    }
    for (SearchElement el : facetQuery.getElements()) {
      if (el instanceof SearchMetadata) {
        Facet f = facetService.retrieveByIndeyFromCache(((SearchMetadata) el).getIndex());

      } else if (el instanceof SearchPair) {

      }
    }
  }

  public String getAddFacetQuery(String index, String value) throws UnprocessableError {
    SearchQuery q = new SearchFactory(facetQuery)
        .addElement(new SearchMetadata(index, value), LOGICAL_RELATIONS.AND).build();
    return SearchQueryParser.transform2UTF8URL(q);
  }


}
