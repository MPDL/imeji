package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.util.List;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.global.GlobalBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;

import de.mpg.imeji.logic.facet.FacetService;
import de.mpg.imeji.logic.facet.model.Facet;

/**
 * Factory class to buid an {@link AbstractAggregationBuilder}
 * 
 * @author saquet
 *
 */
public class ElasticAggregationFactory {

  public static AbstractAggregationBuilder build(QueryBuilder queryBuilder) {
    List<Facet> facets = new FacetService().retrieveAllFromCache();
    GlobalBuilder gb = AggregationBuilders.global("agg");
    for (Facet facet : facets) {
      String metadataField = getMetadataField(facet.getIndex());
      if (metadataField != null) {
        NestedBuilder nb = AggregationBuilders.nested("nested" + facet.getIndex()).path("metadata");
        FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex())
            .filter(QueryBuilders.boolQuery().must(queryBuilder).must(QueryBuilders
                .termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex()))));
        fb.subAggregation(
            AggregationBuilders.terms(facet.getName()).field(getMetadataField(facet.getIndex())));
        nb.subAggregation(fb);
        gb.subAggregation(nb);
      }
    }
    return gb;
  }

  /**
   * Extract from the search index (for ex: md.title.text) the statement index and return it
   * 
   * @param searchIndex
   * @return
   */
  private static String getMetadataStatementIndex(String searchIndex) {
    return searchIndex.startsWith("md.") ? searchIndex.split("\\.")[1] : searchIndex;
  }

  /**
   * Return the field in elasticsearch
   * 
   * @param searchIndex
   * @return
   */
  private static String getMetadataField(String searchIndex) {
    if (searchIndex.startsWith("md.")) {
      String field = searchIndex.split("\\.").length == 2 ? "text" : searchIndex.split("\\.")[2];
      if ("text".equals(field)) {
        return "metadata." + field + ".exact";
      }
      return "metadata." + field;
    }
    return null;
  }
}
