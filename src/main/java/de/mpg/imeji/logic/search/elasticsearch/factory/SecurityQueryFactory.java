package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.net.URI;
import java.util.List;
import java.util.stream.Collectors;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.Grant;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.security.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.ObjectHelper.ObjectType;

/**
 * Build a Boolean query with a list of all allowed collection and subcollections for one user.
 * <br/>
 * This query can be used to filter a search query for items or for collection. <br/>
 * <br/>
 * There are some options to make the query more specific:
 * <li>Set a user
 * <li>Set the Folder (collection or subcollection) in which the query is executed
 * <li>Set the query for a specific role (the user should be then set)
 * <li>Set the type of user which are search (default is collection)
 * <li>Set if the search query is empty or not
 * 
 * 
 * @author saquet
 *
 */
public class SecurityQueryFactory {
  private boolean searchForCollections = true;
  private GrantType role = null;
  private User user;


  /**
   * Build the query for the imeji security
   * 
   * @return
   */
  public QueryBuilder build() {
    if (SecurityUtil.authorization().isSysAdmin(user) && role == null) {
      return QueryBuilders.matchAllQuery();
    } else if (user != null) {
      return buildLoggedInUserSecurityQuery();
    } else {
      return getStatusQuery();
    }
  }

  /**
   * The security query for a logged in user
   * 
   * @return
   */
  private QueryBuilder buildLoggedInUserSecurityQuery() {
    List<String> collectionUris = getCollectionUris();
    collectionUris = addChildren(collectionUris);
    BoolQueryBuilder q = toQuery(collectionUris);
    if (role == null) {
      q.should(getStatusQuery());
    } else if (role != null && collectionUris.isEmpty()) {
      return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
    }
    return q;
  }

  /**
   * Return the security query related to the status: Released object can be read by everybody
   * 
   * @return
   */
  private QueryBuilder getStatusQuery() {
    return QueryBuilders.termQuery(ElasticFields.STATUS.field(), Status.RELEASED.name());
  }

  /**
   * Return a list of collection uris as a BoolQueryBuilder
   * 
   * @param collectionUris
   * @return
   */
  private BoolQueryBuilder toQuery(List<String> collectionUris) {
    final BoolQueryBuilder q = QueryBuilders.boolQuery();
    collectionUris.stream().forEach(uri -> q.should(QueryBuilders.termQuery(
        searchForCollections ? ElasticFields.ID.field() : ElasticFields.FOLDER.field(), uri)));
    return q;
  }

  /**
   * Return the list of all collections allowed for the user and, if defined, for for the grant
   * 
   * @return
   */
  private List<String> getCollectionUris() {
    List<Grant> grants = ElasticSearchFactoryUtil.getAllGrants(user);
    return filterByRoleAndGetCollectionUri(grants, role);
  }

  /**
   * Set the user executing the query
   * 
   * @param user
   * @return
   */
  public SecurityQueryFactory user(User user) {
    this.user = user;
    return this;
  }

  /**
   * Limit the security query to a specific role.
   * 
   * @param role
   * @return
   */
  public SecurityQueryFactory role(GrantType role) {
    this.role = role;
    return this;
  }


  /**
   * Make a search for items
   * 
   * @return
   */
  public SecurityQueryFactory searchForCollection(boolean b) {
    searchForCollections = b;
    return this;
  }

  /**
   * Get all children of a collection of a Collection
   * 
   * @param collectionsUris
   * @return
   */
  private List<String> addChildren(List<String> collectionsUris) {
    return collectionsUris.stream()
        .flatMap(uri -> new HierarchyService().addAllSubcollections(uri).stream())
        .collect(Collectors.toList());
  }

  /**
   * Filter the Grants by role and return the collection uris for these grants
   * 
   * @param user
   * @param role
   * @return
   */
  private static List<String> filterByRoleAndGetCollectionUri(List<Grant> grants, GrantType role) {
    return grants.stream()
        .filter(
            g -> ObjectHelper.getObjectType(URI.create(g.getGrantFor())) == ObjectType.COLLECTION
                && (role == null || role.isSameOrBigger(g.getGrantType())))
        .map(g -> g.getGrantFor()).collect(Collectors.toList());
  }
}
