package de.mpg.imeji.logic.search.elasticsearch.factory.util;

import co.elastic.clients.elasticsearch._types.aggregations.*;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.ResponseBody;
import co.elastic.clients.json.JsonpUtils;
import com.fasterxml.jackson.databind.node.ObjectNode;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticAggregationFactory;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.facet.model.FacetResult;
import de.mpg.imeji.logic.search.facet.model.FacetResultValue;
import org.apache.logging.log4j.LogManager;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Parse elasticSearchAggregation created by {@link ElasticAggregationFactory}
 * 
 * @author saquet
 *
 */
public class AggregationsParser {

  private final static org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(AggregationsParser.class);

  private AggregationsParser() {

  }

  /**
   * Parse a the Aggregation part of a SearchResponse and return a list of {@link FacetResult}.
   * Note: the aggregation must have been created by {@link ElasticAggregationFactory}
   * 
   * @param resp
   * @return
   */
  public static List<FacetResult> parse(ResponseBody<ObjectNode> resp, SearchObjectTypes... types) {
    /*
    if (SearchObjectTypes.ITEM.equals(types[0])) {
      return parseForItemFacets(resp);
    } else if (SearchObjectTypes.COLLECTION.equals(types[0])) {
      return parseForCollectionFacets(resp);
    }
    
    
    return new ArrayList<FacetResult>();
    */
    return parseForItemFacets(resp);

  }

  /*
  public static List<FacetResult> parseForCollectionFacets(SearchResponse resp) {
    LOGGER.info("Parse for collection facets");
  
      
      List<FacetResult> facetResults = new ArrayList<>();
      if (resp != null && resp.getAggregations() != null) {
        Nested metadata = resp.getAggregations().get("info");
        if (metadata != null) {
          for (Aggregation mdAgg : metadata.getAggregations()) {
            FacetResult facetResult = new FacetResult(getFacetName(mdAgg.getName()), mdAgg.getName());
            if (mdAgg instanceof Filter) {
              Aggregation terms = ((Filter) mdAgg).getAggregations().asList().get(0);
              fillResult(terms, facetResult);
            }
  
  
            facetResults.add(facetResult);
          }
          Filters system = resp.getAggregations().get("system");
          if (system != null) {
            Bucket systemBucket = system.getBucketByKey("all");
            for (Aggregation agg : systemBucket.getAggregations()) {
              FacetResult facetResult = new FacetResult(getFacetName(agg.getName()), agg.getName());
              if (agg instanceof Filters) {
                for (Filters.Bucket bucket : ((Filters) agg).getBuckets()) {
                  facetResult.getValues().add(new FacetResultValue(bucket.getKeyAsString(), bucket.getDocCount()));
                }
              } else if (agg instanceof ParsedStringTerms) {
                for (Terms.Bucket bucket : ((ParsedStringTerms) agg).getBuckets()) {
                  facetResult.getValues().add(new FacetResultValue(bucket.getKeyAsString(), bucket.getDocCount()));
                }
  
              }
              facetResults.add(facetResult);
            }
          }
      
      
      
      
      
    return facetResults;
  
  }
  */

  public static List<FacetResult> parseForItemFacets(ResponseBody<ObjectNode> resp) {

    List<FacetResult> facetResults = new ArrayList<>();
    if (resp != null && resp.aggregations() != null && resp.aggregations().size() > 0) {
      NestedAggregate metadata = resp.aggregations().get("metadata").nested();
      if (metadata != null) {
        for (Map.Entry<String, Aggregate> mdAgg : metadata.aggregations().entrySet()) {
          FacetResult facetResult = new FacetResult(getFacetName(mdAgg.getKey()), mdAgg.getKey());
          if (mdAgg.getValue().isFilter()) {
            Aggregate terms = mdAgg.getValue().filter().aggregations().values().iterator().next();
            //Aggregation terms = ((Filter) mdAgg).getAggregations().asList().get(0);
            fillResult(mdAgg.getKey(), terms, facetResult);
          }


          facetResults.add(facetResult);
        }
        FiltersAggregate system = resp.aggregations().get("system").filters();
        if (system != null) {
          FiltersBucket systemBucket = system.buckets().keyed().get("all");
          for (Map.Entry<String, Aggregate> agg : systemBucket.aggregations().entrySet()) {
            FacetResult facetResult = new FacetResult(getFacetName(agg.getKey()), agg.getKey());
            if (agg.getValue().isFilters()) {
              for (Map.Entry<String, FiltersBucket> bucket : agg.getValue().filters().buckets().keyed().entrySet()) {
                facetResult.getValues().add(new FacetResultValue(bucket.getKey(), bucket.getValue().docCount()));
              }
            } else if (agg.getValue().isSterms()) {
              for (StringTermsBucket bucket : agg.getValue().sterms().buckets().array()) {
                facetResult.getValues().add(new FacetResultValue(bucket.key().stringValue(), bucket.docCount()));
              }

            }
            facetResults.add(facetResult);
          }
        }

        if (resp.aggregations().get(Facet.ITEMS) != null) {
          facetResults.add(parseInternalFacet(resp, Facet.ITEMS));
        }
        if (resp.aggregations().get(Facet.SUBCOLLECTIONS) != null) {
          facetResults.add(parseInternalFacet(resp, Facet.SUBCOLLECTIONS));
        }
        if (resp.aggregations().get(Facet.COLLECTION_ITEMS) != null) {
          facetResults.add(parseInternalFacet(resp, Facet.COLLECTION_ITEMS));
        }
        if (resp.aggregations().get(Facet.COLLECTION_ROOT_ITEMS) != null) {
          facetResults.add(parseInternalFacet(resp, Facet.COLLECTION_ROOT_ITEMS));
        }
      }
    }
    return facetResults;

  }


  private static void fillResult(String key, Aggregate terms, FacetResult facetResult) {
    if (terms.isSterms()) {
      for (Map.Entry<String, StringTermsBucket> bucket : terms.sterms().buckets().keyed().entrySet()) {
        facetResult.getValues().add(new FacetResultValue(bucket.getKey(), bucket.getValue().docCount()));
      }

    } /*else if (terms.isStats()) {
      FacetResultValue result = new FacetResultValue(key, terms.stats().count());
      result.setMax(terms.stats().maxAsString());
      result.setMin(terms.stats().minAsString());
      facetResult.getValues().add(result);
      }*/
    else if (terms.isStats()) {
      FacetResultValue result = new FacetResultValue(key, terms.stats().count());
      double max = terms.stats().max();
      double min = terms.stats().min();

      if (Double.isInfinite(max)) {
        result.setMax("0");
      } else {
        if (facetResult.getIndex().endsWith(".date")) // Date
        {
          String maxDate = Instant.ofEpochMilli(Double.valueOf(terms.stats().max()).longValue()).toString();
          result.setMax(maxDate);
        } else {
          result.setMax(Double.toString(terms.stats().max()));
        }

      }
      if (Double.isInfinite(min)) {
        result.setMin("0");
      } else {
        if (facetResult.getIndex().endsWith(".date")) // Date
        {
          String minDate = Instant.ofEpochMilli(Double.valueOf(terms.stats().min()).longValue()).toString();
          result.setMin(minDate);
        } else {
          result.setMin(Double.toString(terms.stats().min()));
        }


      }


      facetResult.getValues().add(result);
    } else {
      System.out.println("NOT PARSED  METADATA AGGREGATION: " + terms);
    }

  }

  /**
   * Parse the internal facets
   * 
   * @param resp
   * @param facetName
   * @return
   */
  private static FacetResult parseInternalFacet(ResponseBody<ObjectNode> resp, String facetName) {
    FacetResult f = new FacetResult(facetName, facetName);
    f.getValues()
        .add(new FacetResultValue("count", resp.aggregations().get(facetName).filters().buckets().keyed().get(facetName).docCount()));
    return f;
  }

  private static String getFacetName(String index) {
    final FacetService service = new FacetService();
    Facet facet = service.retrieveByIndexFromCache(index);
    return facet != null ? facet.getName() : index;
  }

}
