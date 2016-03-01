package de.mpg.imeji.logic.search.elasticsearch.factory;


import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;

import com.hp.hpl.jena.util.iterator.Filter;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.elasticsearch.util.ElasticSearchUtil;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchSimpleMetadata;
import de.mpg.imeji.logic.search.util.SearchUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.UserGroup;

/**
 * Factory to create an ElasticSearch query from the {@link SearchQuery}
 * 
 * @author bastiens
 * 
 */
public class ElasticQueryFactory {
  private static final Logger LOGGER = Logger.getLogger(ElasticQueryFactory.class);

  /**
   * Build a {@link QueryBuilder} from a {@link SearchQuery}
   * 
   * @param query
   * @return
   * @return
   */
  public static QueryBuilder build(SearchQuery query, String folderUri, String spaceId, User user) {
    buildSearchQuery(query, user);
    return QueryBuilders.boolQuery().must(buildSearchQuery(query, user))
        .must(buildContainerFilter(folderUri)).must(buildSecurityQuery(user, folderUri))
        .must(buildSpaceQuery(spaceId)).must(buildStatusQuery(query, user));
  }

  /**
   * The {@link QueryBuilder} with the search query
   * 
   * @param query
   * @return
   */
  private static QueryBuilder buildSearchQuery(SearchQuery query, User user) {
    if (query == null || query.getElements().isEmpty()) {
      return QueryBuilders.matchAllQuery();
    }
    return buildSearchQuery(query.getElements(), user);
  }

  /**
   * Build a query for the status
   * 
   * @param query
   * @param user
   * @return
   */
  private static QueryBuilder buildStatusQuery(SearchQuery query, User user) {
    if (user == null) {
      // Not Logged in: can only view release objects
      return fieldQuery(ElasticFields.STATUS, Status.RELEASED.name(), SearchOperators.EQUALS,
          false);
    } else if (query != null && hasStatusQuery(query.getElements())) {
      // Don't filter, since it is done later via the searchquery
      return QueryBuilders.matchAllQuery();
    } else {
      // Default = don't view discarded objects
      return fieldQuery(ElasticFields.STATUS, Status.WITHDRAWN.name(), SearchOperators.EQUALS,
          true);
    }

  }

  /**
   * Check if at least on {@link SearchPair} is related to the status. If yes, return true
   * 
   * @param elements
   * @return
   */
  private static boolean hasStatusQuery(List<SearchElement> elements) {
    for (SearchElement e : elements) {
      if (e instanceof SearchPair && ((SearchPair) e).getField() == SearchFields.status) {
        return true;
      } else if (e instanceof SearchGroup && hasStatusQuery(e.getElements())) {
        return true;
      }
    }
    return false;
  }


  /**
   * Return the query for space
   * 
   * @param spaceId
   * @return
   */
  private static QueryBuilder buildSpaceQuery(String spaceId) {
    if (spaceId == null || "".equals(spaceId)) {
      return QueryBuilders.matchAllQuery();
    } else {
      // TODO: implements when folder are indexed as well
      return fieldQuery(ElasticFields.SPACE, spaceId, SearchOperators.EQUALS, false);
    }
  }

  /**
   * Build a {@link QueryBuilder} from a list of {@link SearchElement}
   * 
   * @param elements
   * @return
   */
  private static QueryBuilder buildSearchQuery(List<SearchElement> elements, User user) {
    boolean OR = true;
    BoolQueryBuilder q = QueryBuilders.boolQuery();
    for (SearchElement el : elements) {
      if (el instanceof SearchPair) {
        if (OR) {
          q.should(termQuery((SearchPair) el, user));
        } else {
          q.must(termQuery((SearchPair) el, user));
        }
      } else if (el instanceof SearchLogicalRelation) {
        OR = ((SearchLogicalRelation) el).getLogicalRelation() == LOGICAL_RELATIONS.OR ? true
            : false;
      } else if (el instanceof SearchGroup) {
        if (OR) {
          q.should(buildSearchQuery(((SearchGroup) el).getElements(), user));
        } else {
          q.must(buildSearchQuery(((SearchGroup) el).getElements(), user));
        }
      }
    }
    return q;
  }

  /**
   * Build the security Query according to the user.
   * 
   * @param user
   * @return
   */
  private static QueryBuilder buildSecurityQuery(User user, String folderUri) {
    if (user != null) {
      if (user.isAdmin()) {
        // Admin: can view everything
        return QueryBuilders.matchAllQuery();
      } else {
        // normal user
        Collection<Grant> grants = new ArrayList<Grant>();
        grants.addAll(user.getGrants());
        for (UserGroup g : user.getGroups()) {
          grants.addAll(g.getGrants());
        }
        return buildGrantQuery(grants, null);
      }
    }
    return QueryBuilders.matchAllQuery();
  }

  /**
   * Build a Filter for a container (album or folder): if the containerUri is not null, search
   * result will be filter to this only container
   * 
   * @param containerUri
   * @return
   */
  private static QueryBuilder buildContainerFilter(String containerUri) {
    if (containerUri != null) {
      if (isFolderUri(containerUri)) {
        return fieldQuery(ElasticFields.FOLDER, containerUri, SearchOperators.EQUALS, false);
      } else {
        return fieldQuery(ElasticFields.ALBUM, containerUri, SearchOperators.EQUALS, false);
      }
    }
    return QueryBuilders.matchAllQuery();
  }


  /**
   * Build the query with all Read grants
   * 
   * @param grants
   * @return
   */
  private static QueryBuilder buildGrantQuery(Collection<Grant> grants, GrantType grantType) {
    BoolQueryBuilder q = QueryBuilders.boolQuery();
    // Add query for all release objects
    if (grantType == null) {
      q.should(
          fieldQuery(ElasticFields.STATUS, Status.RELEASED.name(), SearchOperators.EQUALS, false));
    }
    // if granttype is null, set it to READ
    grantType = grantType == null ? GrantType.READ : grantType;
    // Add query for each read grant
    for (Grant g : grants) {
      if (g.asGrantType() == grantType) {
        q.should(fieldQuery(ElasticFields.FOLDER, g.getGrantFor().toString(),
            SearchOperators.EQUALS, false));
        q.should(fieldQuery(ElasticFields.ID, g.getGrantFor().toString(), SearchOperators.EQUALS,
            false));
      }
    }
    return q;
  }



  /**
   * Create a QueryBuilder with a term filter (see
   * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-term-filter.html)
   * 
   * @param pair
   * @return
   */
  private static QueryBuilder termQuery(SearchPair pair, User user) {
    if (pair instanceof SearchMetadata) {
      return metadataFilter((SearchMetadata) pair);
    }
    SearchFields index = pair.getField();
    switch (index) {
      case alb:
        break;
      case all:
        BoolQueryBuilder f = QueryBuilders.boolQuery()
            .should(fieldQuery(ElasticFields.ALL, pair.getValue(), SearchOperators.REGEX, false));
        if (NumberUtils.isNumber(pair.getValue())) {
          f.should(fieldQuery(ElasticFields.METADATA_NUMBER, pair.getValue(),
              SearchOperators.EQUALS, false));
        }
        return negate(f, pair.isNot());
      case checksum:
        return fieldQuery(ElasticFields.CHECKSUM, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case citation:
        return fieldQuery(ElasticFields.METADATA_TEXT, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case col:
        return fieldQuery(ElasticFields.FOLDER, pair.getValue(), pair.getOperator(), pair.isNot());
      case cone:
        // not indexed
        break;
      case description:
        // not indexed
        break;
      case cont_md:
        // not indexed
        break;
      case cont_person:
        // not indexed
        break;
      case author_familyname:
        // not indexed
        break;
      case author_givenname:
        // not indexed
        break;
      case author_name:
        // not indexed
        break;
      case cont_person_org:
        // not indexed
        break;
      case author_org_name:
        // not indexed
        break;
      case title:
        // not indexed
        break;
      case created:
        break;
      case creator:
        // not indexed
        break;
      case date:
        return fieldQuery(ElasticFields.METADATA_NUMBER, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case editor:
        // not indexed
        break;
      case filename:
        return fieldQuery(ElasticFields.FILENAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case filetype:
        BoolQueryBuilder q = QueryBuilders.boolQuery();
        for (String ext : SearchUtils.parseFileTypesAsExtensionList(pair.getValue())) {
          q.should(fieldQuery(ElasticFields.FILENAME, "*." + ext, SearchOperators.REGEX, false));
        }
        return q;
      case grant:
        // same as grant_type
        GrantType grant = pair.getValue().equals("upload") ? GrantType.CREATE
            : GrantType.valueOf(pair.getValue().toUpperCase());
        return buildGrantQuery(user.getGrants(), grant);
      case grant_for:
        // not indexed
        break;
      case grant_type:
        // same as grant
        GrantType grantType = pair.getValue().equals("upload") ? GrantType.CREATE
            : GrantType.valueOf(pair.getValue().toUpperCase());
        return buildGrantQuery(user.getGrants(), grantType);
      case member:
        return fieldQuery(ElasticFields.MEMBER, pair.getValue(), pair.getOperator(), pair.isNot());
      case label:
        // not indexed
        break;
      case license:
        return fieldQuery(ElasticFields.METADATA_TEXT, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case md:
        // not indexed
        break;
      case mds:
        // not indexed
        break;
      case modified:
        break;
      case number:
        return fieldQuery(ElasticFields.METADATA_NUMBER, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case person:
        // not indexed
        break;
      case person_family:
        return fieldQuery(ElasticFields.METADATA_FAMILYNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case person_given:
        return fieldQuery(ElasticFields.METADATA_GIVENNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case person_id:
        // not indexed
        break;
      case person_completename:
        // not indexed
        break;
      case person_org:
        // not indexed
        break;
      case person_org_city:
        // not indexed
        break;
      case person_org_country:
        // not indexed
        break;
      case person_org_description:
        // not indexed
        break;
      case person_org_id:
        // not indexed
        break;
      case person_org_name:
        // not indexed
        break;
      case person_role:
        // not indexed
        break;
      case prof:
        return fieldQuery(ElasticFields.PROFILE, pair.getValue(), pair.getOperator(), pair.isNot());
      case prop:
        // not indexed
        break;
      case statement:
        String statementID = ObjectHelper.getId(URI.create(pair.getValue()));
        return fieldQuery(ElasticFields.METADATA_STATEMENT, statementID, pair.getOperator(),
            pair.isNot());
      case status:
        // transform http://imeji.org/terms/status#RELEASED into RELEASED
        String status = pair.getValue();
        if (status.contains("#")) {
          status = status.split("#")[1];
        }
        return fieldQuery(ElasticFields.STATUS, status, pair.getOperator(), pair.isNot());
      case text:
        return fieldQuery(ElasticFields.METADATA_TEXT, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case time:
        // not indexed
        break;
      case location:
        // not indexed
        break;
      case metadatatype:
        return fieldQuery(ElasticFields.METADATA_TYPE, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case url:
        return fieldQuery(ElasticFields.METADATA_URI, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case hasgrant:
        // TODO: with current indexed data, it is only possible to search for the creator. Grants
        // should be indexed to get all objects where the user has grant for
        return fieldQuery(ElasticFields.CREATOR, pair.getValue(), pair.getOperator(), pair.isNot());
      case visibility:
        // not indexed
        break;
      case coordinates:
        break;
      case pid:
        return fieldQuery(ElasticFields.PID, pair.getValue(), pair.getOperator(), pair.isNot());
      default:
        break;
    }
    return matchNothing();
  }

  /**
   * TODO Index Labels of metadata to search for metadata by label
   * 
   * @param md
   * @return
   */
  private static QueryBuilder metadataFilter(SearchSimpleMetadata md) {
    return null;
  }


  /**
   * Create a {@link QueryBuilder} for a {@link SearchMetadata}
   * 
   * @param md
   * @return
   */
  private static QueryBuilder metadataFilter(SearchMetadata md) {
    switch (md.getField()) {
      case text:
        return metadataQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case citation:
        return metadataQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case number:
        return metadataQuery(ElasticFields.METADATA_NUMBER, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case date:
        return metadataQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case url:
        return metadataQuery(ElasticFields.METADATA_URI, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case person_family:
        return metadataQuery(ElasticFields.METADATA_FAMILYNAME, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case person_given:
        return metadataQuery(ElasticFields.METADATA_GIVENNAME, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
      case coordinates:
        return metadataQuery(ElasticFields.METADATA_LOCATION, md.getValue(), SearchOperators.GEO,
            md.getStatement(), md.isNot());
      case time:
        try {
          return metadataQuery(ElasticFields.METADATA_NUMBER,
              Long.toString(SearchUtils.parseDateAsTime(md.getValue())), md.getOperator(),
              md.getStatement(), md.isNot());
        } catch (UnprocessableError e) {
          LOGGER.error("Wrong date format (can not be search): " + md.getValue());
          return QueryBuilders.matchAllQuery();
        }
      default:
        return metadataQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(),
            md.getStatement(), md.isNot());
    }
  }

  /**
   * Create a {@link QueryBuilder}
   * 
   * @param index
   * @param value
   * @param operator
   * @return
   */
  private static QueryBuilder fieldQuery(ElasticFields field, String value,
      SearchOperators operator, boolean not) {
    QueryBuilder q = null;

    if (operator == null) {
      operator = SearchOperators.REGEX;
    }
    switch (operator) {
      case REGEX:
        q = matchFieldQuery(field, ElasticSearchUtil.escape(value));
        break;
      case EQUALS:
        q = exactFieldQuery(field, value);
        break;
      case GREATER:
        q = greaterThanQuery(field, value);
        break;
      case LESSER:
        q = lessThanQuery(field, value);
        break;
      case GEO:
        q = geoQuery(value);
        break;
      default:
        // default is REGEX
        q = matchFieldQuery(field, value);
        break;
    }
    return negate(q, not);
  }

  /**
   * Create a {@link QueryBuilder} - used to sarch for metadata which are defined with a statement
   * 
   * @param index
   * @param value
   * @param operator
   * @param statement
   * @return
   */
  private static QueryBuilder metadataQuery(ElasticFields field, String value,
      SearchOperators operator, URI statement, boolean not) {
    return QueryBuilders.nestedQuery(ElasticFields.METADATA.field(),
        QueryBuilders.boolQuery().must(fieldQuery(field, value, operator, not))
            .must(fieldQuery(ElasticFields.METADATA_STATEMENT, ObjectHelper.getId(statement),
                SearchOperators.EQUALS, false)));

  }

  /**
   * Search for the exact value of a field
   * 
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder exactFieldQuery(ElasticFields field, String value) {
    return QueryBuilders.termQuery(field.fieldExact(), value);
  }

  /**
   * Search for a match (not the exact value)
   * 
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder matchFieldQuery(ElasticFields field, String value) {
    if (field == ElasticFields.ALL) {
      return QueryBuilders.queryStringQuery(value);
    }
    return QueryBuilders.queryStringQuery(field.field() + ":" + value);
  }

  /**
   * Search for value greater than the searched value
   * 
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder greaterThanQuery(ElasticFields field, String value) {
    if (NumberUtils.isNumber(value)) {
      return QueryBuilders.rangeQuery(field.field()).gte(Double.parseDouble(value));
    }
    return matchNothing();
  }

  /**
   * Search for value smaller than searched value
   * 
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder lessThanQuery(ElasticFields field, String value) {
    if (NumberUtils.isNumber(value)) {
      return QueryBuilders.rangeQuery(field.field()).lte(Double.parseDouble(value));
    }
    return matchNothing();
  }

  private static QueryBuilder geoQuery(String value) {
    String[] values = value.split(",");
    String distance = "1km";
    double lat = Double.parseDouble(values[0]);
    double lon = Double.parseDouble(values[1]);
    if (values.length == 3) {
      distance = values[2];
    }
    return QueryBuilders.geoDistanceQuery(ElasticFields.METADATA_LOCATION.field())
        .distance(distance).point(lat, lon);
  }

  /**
   * Add NOT filter to the {@link Filter} if not is true
   * 
   * @param f
   * @param not
   * @return
   */
  private static QueryBuilder negate(QueryBuilder f, boolean not) {
    return not ? QueryBuilders.notQuery(f) : f;
  }

  /**
   * Return a query which find nothing
   * 
   * @return
   */
  private static QueryBuilder matchNothing() {
    return QueryBuilders.notQuery(QueryBuilders.matchAllQuery());
  }

  /**
   * True if the uri is an uri folder
   * 
   * @param uri
   * @return
   */
  private static boolean isFolderUri(String uri) {
    return uri.contains("/collection/") ? true : false;
  }

}
