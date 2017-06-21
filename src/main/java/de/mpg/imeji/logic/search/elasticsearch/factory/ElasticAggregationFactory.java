package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.util.ArrayList;
import java.util.List;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AbstractAggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.filter.FilterAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.filters.FiltersAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.NestedBuilder;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiFileTypes;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.facet.FacetService;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.model.SearchFields;

/**
 * Factory class to buid an {@link AbstractAggregationBuilder}
 * 
 * @author saquet
 *
 */
public class ElasticAggregationFactory {

  public static List<AbstractAggregationBuilder> build(QueryBuilder queryBuilder) {
    List<AbstractAggregationBuilder> aggregations = new ArrayList<>();
    List<Facet> facets = new FacetService().retrieveAllFromCache();
    FiltersAggregationBuilder systemAggregations =
        AggregationBuilders.filters("system").filter("all", QueryBuilders.matchAllQuery());
    NestedBuilder metadataAggregations = AggregationBuilders.nested("metadata").path("metadata");
    for (Facet facet : facets) {
      String metadataField = getMetadataField(facet.getIndex());
      if (metadataField != null) {
        FilterAggregationBuilder fb = AggregationBuilders.filter(facet.getIndex()).filter(
            QueryBuilders.termQuery("metadata.index", getMetadataStatementIndex(facet.getIndex())));
        fb.subAggregation(
            AggregationBuilders.terms(facet.getName()).field(getMetadataField(facet.getIndex())));
        metadataAggregations.subAggregation(fb);
      } else if (SearchFields.filetype.name().equals(facet.getIndex())) {
        FiltersAggregationBuilder filetypeAggregation =
            AggregationBuilders.filters(SearchFields.filetype.name());

        for (ImejiFileTypes.Type type : Imeji.CONFIG.getFileTypes().getTypes()) {
          BoolQueryBuilder filetypeQuery = QueryBuilders.boolQuery();
          for (String ext : type.getExtensionArray()) {
            filetypeQuery.should(QueryBuilders
                .queryStringQuery(ElasticFields.NAME.field() + ".suggest:" + "*." + ext));
          }
          filetypeAggregation.filter(type.getName(null), filetypeQuery);
        }
        systemAggregations.subAggregation(filetypeAggregation);

      }
    }
    aggregations.add(metadataAggregations);
    aggregations.add(systemAggregations);
    return aggregations;
  }

  /**
   * Extract from the search index (for ex: md.title.text) the statement index and return it
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataStatementIndex(String searchIndex) {
    return searchIndex.startsWith("md.") ? searchIndex.split("\\.")[1] : searchIndex;
  }

  /**
   * Return the field in elasticsearch
   * 
   * @param searchIndex
   * @return
   */
  public static String getMetadataField(String searchIndex) {
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
