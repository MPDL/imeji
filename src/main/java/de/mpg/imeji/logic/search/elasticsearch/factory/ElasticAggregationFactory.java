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
      NestedBuilder nb = AggregationBuilders.nested("nested" + facet.getIndex()).path("metadata");
      FilterAggregationBuilder fb = AggregationBuilders.filter("index" + facet.getIndex())
          .filter(QueryBuilders.boolQuery().must(queryBuilder)
              .must(QueryBuilders.termQuery("metadata.index", facet.getIndex())));
      if ("TEXT".equals(facet.getType())) {
        fb.subAggregation(AggregationBuilders.terms(facet.getName()).field("metadata.text.exact"));
      }
      nb.subAggregation(fb);
      gb.subAggregation(nb);
    }
    return gb;
  }
}
