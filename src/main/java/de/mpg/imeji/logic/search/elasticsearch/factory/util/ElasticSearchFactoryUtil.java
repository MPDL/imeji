package de.mpg.imeji.logic.search.elasticsearch.factory.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SortCriterion;

/**
 * Utility Class for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticSearchFactoryUtil {

  /**
   * Read the field of an object in Elasticsearch. The value is returned as String
   *
   * @param id
   * @param field
   * @param dataType
   * @param index
   * @return
   */
  public static String readFieldAsString(String id, ElasticFields field, String dataType,
      String index) {
    Map<String, Object> sourceMap = ElasticService.getClient().prepareGet(index, dataType, id)
        .setFetchSource(true).execute().actionGet().getSource();
    if (sourceMap != null) {
      Object obj = sourceMap.get(field.field());
      return obj != null ? obj.toString() : "";
    }
    return "";
  }

  /**
   * Retrieve the Id of the user according to its email
   * 
   * @param email
   * @return
   */
  public static String getUserId(String email) {
    List<String> r = searchStringAndRetrieveFieldValue("email:\"" + email.toString() + "\"",
        ElasticFields.ID.field().toLowerCase(), null, 0, 1);
    if (r.size() > 0) {
      return r.get(0);
    }
    return null;
  }

  /**
   * Retrieve all usergroup of one user
   * 
   * @param userId
   * @return
   */
  public static List<String> getGroupsOfUser(String userId) {
    return searchStringAndRetrieveFieldValue("users:\"" + userId + "\"",
        ElasticFields.ID.field().toLowerCase(), null, 0, -1);
  }

  /**
   * Search for a String query and retrieve only the value of a specific field
   * 
   * @param query
   * @param field
   * @param sort
   * @param user
   * @param from
   * @param size
   * @return
   */
  public static List<String> searchStringAndRetrieveFieldValue(String query, String field,
      SortCriterion sort, int from, int size) {
    QueryBuilder q = QueryBuilders.queryStringQuery(query);
    SearchResponse resp = ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
        .addField(field).setQuery(q).addSort(ElasticSortFactory.build(sort)).setSize(size)
        .setFrom(from).execute().actionGet();
    List<String> fieldValues = new ArrayList<>();
    for (SearchHit hit : resp.getHits()) {
      if (field.equals(ElasticFields.ID.field())) {
        fieldValues.add(hit.getId());
      } else {
        fieldValues.add(hit.field(field).getValue());
      }
    }
    return fieldValues;
  }

  /**
   * Escape input to avoid error in Elasticsearch. * and ? are unescaped, to allow wildcard search
   *
   * @param s
   * @return
   */
  public static String escape(String s) {
    return QueryParserBase.escape(s).replace("\\*", "*").replace("\\?", "?").replace("\\\"", "\"");
  }
}
