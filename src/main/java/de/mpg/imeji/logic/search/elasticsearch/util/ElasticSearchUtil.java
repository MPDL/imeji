package de.mpg.imeji.logic.search.elasticsearch.util;

import java.util.Map;

import org.apache.lucene.queryparser.classic.QueryParserBase;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SearchResult;

/**
 * Utility Class for ElasticSearch
 *
 * @author bastiens
 *
 */
public class ElasticSearchUtil {

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
    Map<String, Object> sourceMap = ElasticService.client.prepareGet(index, dataType, id)
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
    ElasticSearch search = new ElasticSearch(SearchObjectTypes.USER);
    SearchResult r = search.searchStringAndRetrieveFieldValue("email:\"" + email.toString() + "\"",
        ElasticFields.ID.field().toLowerCase(), null, Imeji.adminUser, 0, 1);
    if (r.getNumberOfRecords() > 0) {
      return r.getResults().get(0);
    }
    return null;
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
