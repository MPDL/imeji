package de.mpg.imeji.logic.search.elasticsearch.factory;


import java.util.List;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.log4j.Logger;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchAllQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.RangeQueryBuilder;

import com.hp.hpl.jena.util.iterator.Filter;

import de.mpg.imeji.logic.authorization.util.SecurityUtil;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchTechnicalMetadata;
import de.mpg.imeji.logic.search.util.SearchUtils;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.ImejiLicenses;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;

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
  public static QueryBuilder build(SearchQuery query, String folderUri, User user,
      ElasticTypes type) {
    final BoolQueryBuilder q = QueryBuilders.boolQuery();
    final QueryBuilder searchQuery = buildSearchQuery(query, user);
    final QueryBuilder containerQuery = buildContainerFilter(folderUri);
    final QueryBuilder securityQuery = buildSecurityQuery(user, folderUri);
    final QueryBuilder statusQuery = buildStatusQuery(query, user);
    if (!isMatchAll(searchQuery)) {
      q.must(searchQuery);
    }
    if (!isMatchAll(containerQuery)) {
      q.must(containerQuery);
    }
    if (!isMatchAll(securityQuery)) {
      q.must(securityQuery);
    }
    if (type != ElasticTypes.users && !isMatchAll(statusQuery)) {
      q.must(statusQuery);
    }
    return q;
  }

  /**
   * True if the query is a match all query
   *
   * @param q
   * @return
   */
  private static boolean isMatchAll(QueryBuilder q) {
    return q instanceof MatchAllQueryBuilder;
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
    for (final SearchElement e : elements) {
      if (e instanceof SearchPair && ((SearchPair) e).getField() == SearchFields.status) {
        return true;
      } else if (e instanceof SearchGroup && hasStatusQuery(e.getElements())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Build a {@link QueryBuilder} from a list of {@link SearchElement}
   *
   * @param elements
   * @return
   */
  private static QueryBuilder buildSearchQuery(List<SearchElement> elements, User user) {
    boolean OR = isORSearchGroup(elements);
    final BoolQueryBuilder q = QueryBuilders.boolQuery();
    for (final SearchElement el : elements) {
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
        boolean not = ((SearchGroup) el).isNot();
        if (OR) {
          q.should(negate(buildSearchQuery(((SearchGroup) el).getElements(), user), not));
        } else {
          q.must(negate(buildSearchQuery(((SearchGroup) el).getElements(), user), not));
        }
      }
    }
    return q;
  }

  private static boolean isORSearchGroup(List<SearchElement> elements) {
    for (final SearchElement el : elements) {
      if (el instanceof SearchLogicalRelation) {
        return ((SearchLogicalRelation) el).getLogicalRelation() == LOGICAL_RELATIONS.OR ? true
            : false;
      }
    }
    return true;
  }

  /**
   * Build the security Query according to the user.
   *
   * @param user
   * @return
   */
  private static QueryBuilder buildSecurityQuery(User user, String folderUri) {
    if (user != null) {
      if (SecurityUtil.authorization().isSysAdmin(user)) {
        // Admin: can view everything
        return QueryBuilders.matchAllQuery();
      } else {
        // normal user
        return buildGrantQuery(user, null);
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
      return fieldQuery(ElasticFields.FOLDER, containerUri, SearchOperators.EQUALS, false);
    }
    return QueryBuilders.matchAllQuery();
  }


  /**
   * Build the query with all Read grants
   *
   * @param grants
   * @return
   */
  private static QueryBuilder buildGrantQuery(User user, GrantType role) {
    final BoolQueryBuilder q = QueryBuilders.boolQuery();
    // Add query for all release objects
    if (role == null) {
      q.should(
          fieldQuery(ElasticFields.STATUS, Status.RELEASED.name(), SearchOperators.EQUALS, false));
    }
    if (role == GrantType.EDIT) {
      q.should(roleQuery(ElasticFields.UPLOAD, user.getEmail(), false));
    } else {
      q.should(roleQuery(ElasticFields.READ, user.getEmail(), false));
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
    } else if (pair instanceof SearchTechnicalMetadata) {
      return technicalMetadataQuery((SearchTechnicalMetadata) pair);
    }
    final SearchFields index = pair.getField();
    switch (index) {
      case all:
        return allQuery(pair);
      case fulltext:
        return contentQuery(
            fieldQuery(ElasticFields.FULLTEXT, pair.getValue(), pair.getOperator(), pair.isNot()));
      case checksum:
        return contentQuery(
            fieldQuery(ElasticFields.CHECKSUM, pair.getValue(), pair.getOperator(), pair.isNot()));
      case col:
        return fieldQuery(ElasticFields.FOLDER, pair.getValue(), pair.getOperator(), pair.isNot());
      case collectionid:
      case collection:
        return fieldQuery(ElasticFields.FOLDER, "\"" + ObjectHelper
            .getURI(CollectionImeji.class, pair.getValue().replaceAll("\"", "")).toString() + "\"",
            pair.getOperator(), pair.isNot());
      case title:
        return fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case description:
        return fieldQuery(ElasticFields.DESCRIPTION, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case author_familyname:
        return fieldQuery(ElasticFields.AUTHOR_FAMILYNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case author_givenname:
        return fieldQuery(ElasticFields.AUTHOR_GIVENNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case author:
        return fieldQuery(ElasticFields.AUTHOR_COMPLETENAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case author_organization:
        return fieldQuery(ElasticFields.AUTHOR_ORGANIZATION, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case collection_title:
        return parentCollectionQuery(
            fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot()));
      case collection_description:
        return parentCollectionQuery(fieldQuery(ElasticFields.AUTHOR_COMPLETENAME, pair.getValue(),
            pair.getOperator(), pair.isNot()));
      case collection_author:
        return fieldQuery(ElasticFields.AUTHORS_OF_COLLECTION, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case collection_author_organisation:
        return fieldQuery(ElasticFields.ORGANIZATION_OF_COLLECTION, pair.getValue(),
            pair.getOperator(), pair.isNot());
      case family:
        return fieldQuery(ElasticFields.FAMILYNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case given:
        return fieldQuery(ElasticFields.GIVENNAME, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case organization:
        return fieldQuery(ElasticFields.ORGANIZATION, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case created:
        return timeQuery(ElasticFields.CREATED.field(), pair.getValue(), pair.getOperator(),
            pair.isNot());
      case creator:
        return fieldQuery(ElasticFields.CREATOR,
            ElasticSearchFactoryUtil.getUserId(pair.getValue()), pair.getOperator(), pair.isNot());
      case collaborator:
        return roleQueryWithoutCreator(ElasticFields.READ, pair.getValue(), pair.isNot());
      case read:
        return fieldQuery(ElasticFields.READ, pair.getValue(), SearchOperators.EQUALS,
            pair.isNot());
      case filename:
        return fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case filetype:
        return fileTypeQuery(pair);
      case role:
        // same as grant_type
        final GrantType grant = pair.getValue().equals("upload") ? GrantType.EDIT
            : GrantType.valueOf(pair.getValue().toUpperCase());
        return buildGrantQuery(user, grant);
      case license:
        return licenseQuery(pair);
      case modified:
        return timeQuery(ElasticFields.MODIFIED.field(), pair.getValue(), pair.getOperator(),
            pair.isNot());
      case status:
        return fieldQuery(ElasticFields.STATUS, formatStatusSearchValue(pair), pair.getOperator(),
            pair.isNot());
      case pid:
        return fieldQuery(ElasticFields.PID, pair.getValue(), pair.getOperator(), pair.isNot());
      case info_label:
        return fieldQuery(ElasticFields.INFO_LABEL, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case info_text:
        return fieldQuery(ElasticFields.INFO_TEXT, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case info_url:
        return fieldQuery(ElasticFields.INFO_URL, pair.getValue(), pair.getOperator(),
            pair.isNot());
      case email:
        return fieldQuery(ElasticFields.EMAIL, pair.getValue(), SearchOperators.EQUALS,
            pair.isNot());
      case index:
        return fieldQuery(ElasticFields.METADATA_INDEX, pair.getValue(), SearchOperators.EQUALS,
            pair.isNot());
      case completename:
        break;
      case creatorid:
        break;
      case editor:
        break;
      case filesize:
        break;
      case id:
        return fieldQuery(ElasticFields.IDSTRING, pair.getValue(), SearchOperators.EQUALS,
            pair.isNot());
      case md:
        break;
      case technical:
        break;
      default:
        break;
    }
    return matchNothing();
  }

  /**
   * Create a {@link QueryBuilder} for a {@link SearchMetadata}
   *
   * @param md
   * @return
   */
  private static QueryBuilder metadataFilter(SearchMetadata md) {
    if (md.getMetadataField() == null) {
      return metadataQuery(
          fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()),
          md.getIndex());
    }
    switch (md.getMetadataField()) {
      case exact:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_EXACT, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case text:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case placename:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_NAME, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case title:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_TITLE, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case number:
        return metadataQuery(numberQuery(ElasticFields.METADATA_NUMBER.field(), md.getValue(),
            md.getOperator(), md.isNot()), md.getIndex());
      case date:
        return metadataQuery(timeQuery(ElasticFields.METADATA_TIME.field(), md.getValue(),
            md.getOperator(), md.isNot()), md.getIndex());
      case url:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_URI, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case familyname:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_FAMILYNAME, md.getValue(),
            md.getOperator(), md.isNot()), md.getIndex());
      case givenname:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_GIVENNAME, md.getValue(),
            md.getOperator(), md.isNot()), md.getIndex());
      case coordinates:
        return metadataQuery(geoQuery(md.getValue()), md.getIndex());
      case time:
        return metadataQuery(timeQuery(ElasticFields.METADATA_TIME.field(), md.getValue(),
            md.getOperator(), md.isNot()), md.getIndex());
      default:
        return metadataQuery(
            fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
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
    return fieldQuery(field.field(), value, operator, not);
  }

  /**
   * Create a {@link QueryBuilder}
   *
   * @param index
   * @param value
   * @param operator
   * @return
   */
  private static QueryBuilder fieldQuery(String fieldName, String value, SearchOperators operator,
      boolean not) {
    QueryBuilder q = null;

    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case EQUALS:
        q = matchFieldQuery(fieldName, ElasticSearchFactoryUtil.escape(value));
        break;
      case GREATER:
        q = greaterThanQuery(fieldName, value);
        break;
      case LESSER:
        q = lessThanQuery(fieldName, value);
        break;
      default:
        // default is REGEX
        q = matchFieldQuery(fieldName, value);
        break;
    }
    return negate(q, not);
  }

  /**
   * Search for a date saved as a time (i.e) in ElasticSearch
   *
   * @param field
   * @param dateString
   * @param operator
   * @param not
   * @return
   */
  private static QueryBuilder timeQuery(String field, String dateString, SearchOperators operator,
      boolean not) {
    QueryBuilder q = null;
    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case GREATER:
        q = greaterThanQuery(field, Long.toString(DateFormatter.getTime(dateString)));
        break;
      case LESSER:
        q = lessThanQuery(field, Long.toString(DateFormatter.getTime(dateString)));
        break;
      default:
        String from = parseFromValue(dateString);
        String to = parseToValue(dateString);
        RangeQueryBuilder rq = QueryBuilders.rangeQuery(field);
        if (!StringHelper.isNullOrEmptyTrim(from)) {
          rq.gte(DateFormatter.getTime(from));
        }
        if (!StringHelper.isNullOrEmptyTrim(to)) {
          rq.lt(DateFormatter.getTime(to));
        }
        q = rq;
        break;
    }
    return negate(q, not);
  }



  private static QueryBuilder numberQuery(String field, String number, SearchOperators operator,
      boolean not) {
    QueryBuilder q = null;
    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case GREATER:
        q = greaterThanQuery(field, number);
        break;
      case LESSER:
        q = lessThanQuery(field, number);
        break;
      default:
        String from = parseFromValue(number);
        String to = parseToValue(number);
        RangeQueryBuilder rq = QueryBuilders.rangeQuery(field);
        if (!StringHelper.isNullOrEmptyTrim(from)) {
          rq.gte(from);
        }
        if (!StringHelper.isNullOrEmptyTrim(to)) {
          rq.lte(to);
        }
        q = rq;
        break;
    }
    return negate(q, not);
  }


  /**
   * Parse the from value in a query with the format: from 123 to 456
   * 
   * @param fromToQueryString
   * @return
   */
  private static String parseFromValue(String fromToQueryString) {
    int fromIndex = fromToQueryString.indexOf("from");
    int toIndex = fromToQueryString.indexOf("to");
    toIndex = toIndex != -1 ? toIndex : fromToQueryString.length();
    return fromIndex != -1 ? fromToQueryString.substring(fromIndex + "from".length(), toIndex)
        : null;
  }


  /**
   * Parse the to value in a query with the format: from 123 to 456
   * 
   * @param fromToQueryString
   * @return
   */
  private static String parseToValue(String fromToQueryString) {
    int toIndex = fromToQueryString.indexOf("to");
    return toIndex != -1 ? fromToQueryString.substring(toIndex + "to".length()) : null;
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
  private static QueryBuilder metadataQuery(QueryBuilder valueQuery, String statement) {
    return QueryBuilders.nestedQuery(ElasticFields.METADATA.field(),
        QueryBuilders.boolQuery().must(valueQuery).must(
            fieldQuery(ElasticFields.METADATA_INDEX, statement, SearchOperators.EQUALS, false)));

  }

  /**
   * Query for technical metadata
   *
   * @param label
   * @param value
   * @param not
   * @return
   */
  private static QueryBuilder technicalMetadataQuery(SearchTechnicalMetadata tmd) {
    return contentQuery(QueryBuilders.nestedQuery(ElasticFields.TECHNICAL.field(),
        QueryBuilders.boolQuery()
            .must(fieldQuery(ElasticFields.TECHNICAL_NAME, tmd.getLabel(), SearchOperators.EQUALS,
                false))
            .must(fieldQuery(ElasticFields.TECHNICAL_VALUE, tmd.getValue(), tmd.getOperator(),
                tmd.isNot()))));
  }

  /**
   * Search for a match (not the exact value)
   *
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder matchFieldQuery(String fieldName, String value) {
    if (ElasticFields.ALL.field().equalsIgnoreCase(fieldName)) {
      return QueryBuilders
          .queryStringQuery(value + " " + ElasticFields.NAME.field() + ".suggest:" + value);
    }
    return QueryBuilders.queryStringQuery(fieldName + ":" + value);
  }

  /**
   * Search for value greater than the searched value
   *
   * @param field
   * @param value
   * @return
   */
  private static QueryBuilder greaterThanQuery(String fieldName, String value) {
    if (NumberUtils.isNumber(value)) {
      return QueryBuilders.rangeQuery(fieldName).gte(Double.parseDouble(value));
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
  private static QueryBuilder lessThanQuery(String fieldName, String value) {
    if (NumberUtils.isNumber(value)) {
      return QueryBuilders.rangeQuery(fieldName).lte(Double.parseDouble(value));
    }
    return matchNothing();
  }


  private static QueryBuilder geoQuery(String value) {
    final String[] values = value.split(",");
    String distance = "1cm";
    final double lat = Double.parseDouble(values[0]);
    final double lon = Double.parseDouble(values[1]);
    if (values.length == 3) {
      distance =
          (values[2].equals("0") || values[2].matches("[0]+[a-zA-Z]{1,2}$")) ? distance : values[2];
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
    return not ? QueryBuilders.boolQuery().mustNot(f) : f;
  }

  /**
   * Return a query which find nothing
   *
   * @return
   */
  private static QueryBuilder matchNothing() {
    return QueryBuilders.boolQuery().mustNot(QueryBuilders.matchAllQuery());
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

  /**
   * Create the query for role="email". Role can be uploader, collaborator. Objects where the user
   * is creator are not excluded
   *
   * @param email
   * @param not
   * @return
   */
  private static QueryBuilder roleQuery(ElasticFields role, String email, boolean not) {
    final String userId = ElasticSearchFactoryUtil.getUserId(email);
    final QueryBuilder q1 = QueryBuilders.termsLookupQuery(ElasticFields.ID.field())
        .lookupIndex(ElasticService.DATA_ALIAS).lookupId(userId)
        .lookupType(ElasticTypes.users.name()).lookupPath(role.field());
    final QueryBuilder q2 = QueryBuilders.termsLookupQuery(ElasticFields.FOLDER.field())
        .lookupIndex(ElasticService.DATA_ALIAS).lookupId(userId)
        .lookupType(ElasticTypes.users.name()).lookupPath(role.field());
    final BoolQueryBuilder q = QueryBuilders.boolQuery().should(q1).should(q2);
    final List<String> groups = ElasticSearchFactoryUtil.getGroupsOfUser(userId);
    for (final String group : groups) {
      q.should(QueryBuilders.termsLookupQuery(ElasticFields.ID.field())
          .lookupIndex(ElasticService.DATA_ALIAS).lookupId(group)
          .lookupType(ElasticTypes.usergroups.name()).lookupPath(role.field()));
      q.should(QueryBuilders.termsLookupQuery(ElasticFields.FOLDER.field())
          .lookupIndex(ElasticService.DATA_ALIAS).lookupId(group)
          .lookupType(ElasticTypes.usergroups.name()).lookupPath(role.field()));
    }
    return q;
  }



  /**
   * Create the query for role="email". Role can be uploader, collaborator. Objects where the user
   * is creator will be excluded
   *
   * @param email
   * @param not
   * @return
   */
  private static QueryBuilder roleQueryWithoutCreator(ElasticFields role, String email,
      boolean not) {
    final String userId = ElasticSearchFactoryUtil.getUserId(email);
    final BoolQueryBuilder q1 = QueryBuilders.boolQuery();
    q1.must(QueryBuilders.termsLookupQuery(ElasticFields.ID.field())
        .lookupIndex(ElasticService.DATA_ALIAS).lookupId(userId)
        .lookupType(ElasticTypes.users.name()).lookupPath(role.field()));
    q1.mustNot(fieldQuery(ElasticFields.CREATOR, ElasticSearchFactoryUtil.getUserId(email),
        SearchOperators.EQUALS, not));
    final BoolQueryBuilder q2 = QueryBuilders.boolQuery();
    q2.must(QueryBuilders.termsLookupQuery(ElasticFields.FOLDER.field())
        .lookupIndex(ElasticService.DATA_ALIAS).lookupId(userId)
        .lookupType(ElasticTypes.users.name()).lookupPath(role.field()));
    q2.mustNot(fieldQuery(ElasticFields.CREATOR, ElasticSearchFactoryUtil.getUserId(email),
        SearchOperators.EQUALS, not));
    final BoolQueryBuilder q = QueryBuilders.boolQuery().should(q1).should(q2);
    final List<String> groups = ElasticSearchFactoryUtil.getGroupsOfUser(userId);
    for (final String group : groups) {
      q.should(QueryBuilders.termsLookupQuery(ElasticFields.ID.field())
          .lookupIndex(ElasticService.DATA_ALIAS).lookupId(group)
          .lookupType(ElasticTypes.usergroups.name()).lookupPath(role.field()));
    }
    return q;
  }

  /**
   * Build Query for all terms
   * 
   * @param pair
   * @return
   */
  private static QueryBuilder allQuery(SearchPair pair) {
    final BoolQueryBuilder f = QueryBuilders.boolQuery()
        .should(fieldQuery(ElasticFields.ALL, pair.getValue(), SearchOperators.EQUALS, false));
    if (NumberUtils.isNumber(pair.getValue())) {
      f.should(fieldQuery(ElasticFields.METADATA_NUMBER, pair.getValue(), SearchOperators.EQUALS,
          false));
    }
    return negate(f, pair.isNot());
  }

  /**
   * Search for content
   * 
   * @param q
   * @return
   */
  private static QueryBuilder contentQuery(QueryBuilder q) {
    return QueryBuilders.hasChildQuery(ElasticTypes.content.name(), q);
  }

  /**
   * Make a query for license
   * 
   * @param pair
   * @return
   */
  private static QueryBuilder licenseQuery(SearchPair pair) {
    final BoolQueryBuilder licenseQuery = QueryBuilders.boolQuery();
    for (final String licenseName : pair.getValue().split(" OR ")) {
      if ("*".equals(licenseName)) {
        licenseQuery.mustNot(fieldQuery(ElasticFields.LICENSE, ImejiLicenses.NO_LICENSE,
            SearchOperators.EQUALS, false));
        licenseQuery.should(QueryBuilders.existsQuery(ElasticFields.LICENSE.field()));
      } else {
        licenseQuery
            .should(fieldQuery(ElasticFields.LICENSE, licenseName, SearchOperators.EQUALS, false));
      }
    }
    return licenseQuery;
  }

  private static QueryBuilder parentCollectionQuery(QueryBuilder qb) {
    return QueryBuilders.hasParentQuery(ElasticTypes.folders.name(), qb);
  }

  /**
   * Build a query to search for filetypes
   * 
   * @param pair
   * @return
   */
  private static QueryBuilder fileTypeQuery(SearchPair pair) {
    final BoolQueryBuilder filetypeQuery = QueryBuilders.boolQuery();
    for (final String ext : SearchUtils
        .parseFileTypesAsExtensionList(pair.getValue().replace("\"", ""))) {

      filetypeQuery.should(fieldQuery(ElasticFields.NAME.field() + ".suggest", "*." + ext + "",
          SearchOperators.EQUALS, false));
    }
    return filetypeQuery;

  }

  /**
   * Format the search value for the status, as indexed
   * 
   * @param pair
   * @return
   */
  private static String formatStatusSearchValue(SearchPair pair) {
    String status = pair.getValue();
    if (status.contains("#")) {
      status = status.split("#")[1];
    }
    if ("private".equals(status)) {
      status = Status.PENDING.name();
    }
    if ("public".equals(status)) {
      status = Status.RELEASED.name();
    }
    if ("discarded".equals(status)) {
      status = Status.WITHDRAWN.name();
    }
    return status.toUpperCase();
  }
}
