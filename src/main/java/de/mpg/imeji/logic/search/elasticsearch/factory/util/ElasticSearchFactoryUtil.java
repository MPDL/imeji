package de.mpg.imeji.logic.search.elasticsearch.factory.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;

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
    final Map<String, Object> sourceMap = ElasticService.getClient().prepareGet(index, dataType, id)
        .setFetchSource(true).execute().actionGet().getSource();
    if (sourceMap != null) {
      final Object obj = sourceMap.get(field.field());
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
    final List<String> r = searchStringAndRetrieveFieldValue("email:\"" + email.toString() + "\"",
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
   * Get all Grant of the users, included the grant of its groups
   * 
   * @param user
   * @return
   */
  public static List<Grant> getAllGrants(User user) {
    List<Grant> grants =
        user.getGrants().stream().map(s -> new Grant(s)).collect(Collectors.toList());
    grants.addAll(getGroupGrants(user));
    return grants;
  }

  /**
   * Get the all the grants of the groups of the users
   * 
   * @param user
   * @return
   */
  public static List<Grant> getGroupGrants(User user) {
    UserGroupService ugs = new UserGroupService();
    List<UserGroup> groups = ugs.retrieveBatch(getGroupsOfUser(user.getId().toString()), user);
    return groups.stream().flatMap(group -> group.getGrants().stream()).map(s -> new Grant(s))
        .collect(Collectors.toList());
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
    final QueryBuilder q = QueryBuilders.queryStringQuery(query);
    final SearchResponse resp = ElasticService.getClient().prepareSearch(ElasticService.DATA_ALIAS)
        .addField(field).setQuery(q).addSort(ElasticSortFactory.build(sort)).setSize(size)
        .setFrom(from).execute().actionGet();
    final List<String> fieldValues = new ArrayList<>();
    for (final SearchHit hit : resp.getHits()) {
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
