package de.mpg.imeji.logic.search.elasticsearch.factory.util;

import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.UserGroup;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.elasticsearch.ElasticSearch;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.factory.ElasticSortFactory;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.usergroup.UserGroupService;
import org.apache.lucene.queryparser.classic.QueryParserBase;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.search.sort.SortBuilder;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Utility Class for ElasticSearch Contains common search options for ElasticSearch
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
  public static String readFieldAsString(String id, ElasticFields field, String dataType, String index) {
    GetRequest getRequest = new GetRequest();
    getRequest.index(index).type(dataType).id(id);
    GetResponse getResponse;
    try {
      getResponse = ElasticService.getClient().get(getRequest, RequestOptions.DEFAULT);
      final Map<String, Object> sourceMap = getResponse.getSource();
      if (sourceMap != null) {
        final Object obj = sourceMap.get(field.field());
        return obj != null ? obj.toString() : "";
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
    return "";
  }

  /**
   * Function searches ElasticSearch database Retrieves all documents (of a given type) that have no
   * or an empty field
   * 
   * @param dataType type of documents that will be searched
   * @param nameOfMissingField name of the field
   * @return list of UIDs of documents that miss the field
   */
  /*
   * public static List<String> getDocumentsThatMissAField(String dataType, String
   * nameOfMissingField) {
   * 
   * List<String> resultUIDs = new ArrayList<String>(); Client elasticSearchClient
   * = ElasticService.getClient();
   * 
   * // (1) construction of Query to ElasticSearch SearchRequestBuilder
   * elasticSearchRequestBuilder =
   * elasticSearchClient.prepareSearch(ElasticService.DATA_ALIAS);
   * 
   * // set the scan&scroll search type parameters: size and timeout
   * elasticSearchRequestBuilder =
   * elasticSearchRequestBuilder.setSize(ElasticSearch.SEARCH_SCROLL_INTERVALL);
   * elasticSearchRequestBuilder.setScroll(new
   * TimeValue(ElasticSearch.SCROLL_TIMEOUT_MSEC));
   * 
   * // set the document type elasticSearchRequestBuilder =
   * elasticSearchRequestBuilder.setTypes(dataType);
   * 
   * // set the query final ExistsQueryBuilder eqb = new
   * ExistsQueryBuilder(nameOfMissingField); BoolQueryBuilder bqb = new
   * BoolQueryBuilder(); bqb = bqb.mustNot(eqb); elasticSearchRequestBuilder =
   * elasticSearchRequestBuilder.setQuery(bqb);
   * 
   * // (2) Send query to ElasticSearch: SearchResponse scrollResponse =
   * elasticSearchRequestBuilder.get();
   * 
   * // Scroll until no hits are returned do { for (SearchHit hit :
   * scrollResponse.getHits().getHits()) { // get UIDs from retrieved documents
   * resultUIDs.add(hit.getId()); }
   * 
   * scrollResponse =
   * elasticSearchClient.prepareSearchScroll(scrollResponse.getScrollId())
   * .setScroll(new
   * TimeValue(ElasticSearch.SCROLL_TIMEOUT_MSEC)).execute().actionGet(); } while
   * (scrollResponse.getHits().getHits().length != 0); // Zero hits mark the end
   * of the scroll and the while // loop.
   * 
   * return resultUIDs;
   * 
   * }
   */

  /**
   * Add single or multilevel sorting to an ElasticSearch {@link SearchRequestBuilder}
   * 
   * Add a list of Imeji {@link SortCriterion}. These are used to sort results as follows:
   * 
   * Results are first sorted by the first sort criterion in the list Elements that fall into the
   * same category are then sorted by the second criterion of the list and so on
   * 
   * @param searchRequestBuilder
   * @param sortCriteria
   * @return SearchRequestBuilder with single or multilevel sorting
   */
  public static SearchRequestBuilder addSorting(SearchRequestBuilder searchRequestBuilder, List<SortCriterion> sortCriteria) {

    List<SortBuilder> sortBuilders = ElasticSortFactory.build(sortCriteria);
    for (SortBuilder sortBuilder : sortBuilders) {
      searchRequestBuilder.addSort(sortBuilder);
    }
    return searchRequestBuilder;
  }

  /**
   * Retrieve the Id of the user according to its email
   *
   * @param email
   * @return
   */
  public static String getUserId(String email) {
    final List<String> r = ElasticSearch.searchStringAndRetrieveFieldValue(ElasticIndices.users.name(),
        "email:\"" + email.toString() + "\"", ElasticFields.ID.field().toLowerCase(), null, 0, 1);
    if (r.size() > 0) {
      return r.get(0);
    }
    return null;
  }

  /**
   * Retrieve all user groups of one user
   *
   * @param userId
   * @return
   */
  public static List<String> getGroupsOfUser(String userId) {

    List<String> userGroupsUIds = ElasticSearch.searchStringAndRetrieveFieldValue(ElasticIndices.usergroups.name(),
        "users:\"" + userId + "\"", ElasticFields.ID.field().toLowerCase(), null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
    return userGroupsUIds;
  }

  /**
   * Get all Grants of the users, including the grants of its groups
   * 
   * @param user
   * @return
   */
  public static List<Grant> getAllGrants(User user) {
    List<Grant> grants = user.getGrants().stream().map(s -> new Grant(s)).collect(Collectors.toList());
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
    return groups.stream().flatMap(group -> group.getGrants().stream()).map(s -> new Grant(s)).collect(Collectors.toList());
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
