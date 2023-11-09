package de.mpg.imeji.logic.search.elasticsearch.factory;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
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
import org.reflections.util.QueryBuilder;

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
  public Query build() {
    if (SecurityUtil.authorization().isSysAdmin(user) && role == null) {
      return new MatchAllQuery.Builder().build()._toQuery();
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
  private Query buildLoggedInUserSecurityQuery() {
    List<String> collectionUris = getCollectionUris();
    collectionUris = addChildren(collectionUris);
    BoolQuery.Builder qb = toQuery(collectionUris);
    if (role == null) {
      qb.should(getStatusQuery());
    } else if (role != null && collectionUris.isEmpty()) {
      return BoolQuery.of(bq -> bq.mustNot(new MatchAllQuery.Builder().build()._toQuery()))._toQuery();
      //return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
    }
    return qb.build()._toQuery();
  }

  /**
   * Return the security query related to the status: Released object can be read by everybody
   * 
   * @return
   */
  private Query getStatusQuery() {
    return TermQuery.of(t -> t.field(ElasticFields.STATUS.field()).value(FieldValue.of(Status.RELEASED.name())))._toQuery();
  }

  /**
   * Return a list of collection uris as a BoolQueryBuilder
   * 
   * @param collectionUris
   * @return
   */
  private BoolQuery.Builder toQuery(List<String> collectionUris) {
    final BoolQuery.Builder q = new BoolQuery.Builder();
    //List<FieldValue> fvList = new ArrayList<>();
    List<FieldValue> fvList = collectionUris.stream().map(i -> FieldValue.of(i)).collect(Collectors.toList());
    q.should(TermsQuery
        .of(i -> i.field(searchForCollections ? ElasticFields.ID.field() : ElasticFields.FOLDER.field()).terms(te -> te.value(fvList)))
        ._toQuery());

    //collectionUris.stream().forEach(
    //    uri -> q.should(QueryBuilders.termQuery(searchForCollections ? ElasticFields.ID.field() : ElasticFields.FOLDER.field(), uri)));
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
    return collectionsUris.stream().flatMap(uri -> new HierarchyService().addAllSubcollections(uri).stream()).collect(Collectors.toList());
  }

  /**
   * Filter the Grants by role and return the collection uris for these grants
   * 
   * @param user
   * @param role
   * @return
   */
  private static List<String> filterByRoleAndGetCollectionUri(List<Grant> grants, GrantType role) {
    return grants.stream().filter(g -> ObjectHelper.getObjectType(URI.create(g.getGrantFor())) == ObjectType.COLLECTION
        && (role == null || role.isSameOrBigger(g.getGrantType()))).map(g -> g.getGrantFor()).collect(Collectors.toList());
  }
}
