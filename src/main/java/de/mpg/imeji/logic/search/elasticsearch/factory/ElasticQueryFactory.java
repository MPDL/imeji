package de.mpg.imeji.logic.search.elasticsearch.factory;

import co.elastic.clients.elasticsearch._types.FieldValue;
import co.elastic.clients.elasticsearch._types.query_dsl.*;
import co.elastic.clients.json.JsonData;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.hierarchy.HierarchyService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Grant.GrantType;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.util.StatementUtil;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticIndices;
import de.mpg.imeji.logic.search.elasticsearch.factory.util.ElasticSearchFactoryUtil;
import de.mpg.imeji.logic.search.elasticsearch.model.ElasticFields;
import de.mpg.imeji.logic.search.model.*;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.util.SearchUtils;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.util.DateFormatter;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Factory to create an ElasticSearch query from the {@link SearchQuery}
 *
 * @author bastiens
 *
 */
public class ElasticQueryFactory {
  private final Logger LOGGER = LogManager.getLogger(ElasticQueryFactory.class);
  private final boolean searchForCollection;
  private boolean includeSubcollections;
  private final boolean searchForUsers;
  private final boolean emptyQuery;
  private String folderUri = null;
  private final SearchQuery query;
  private User user = null;

  public ElasticQueryFactory(SearchQuery query, ElasticIndices... types) {
    searchForCollection = types.length == 1 && types[0] == ElasticIndices.folders;
    searchForUsers = types.length == 1 && types[0] == ElasticIndices.users;
    emptyQuery = query == null || query.isEmpty() || query.getElements().isEmpty();
    this.query = query;
  }

  public ElasticQueryFactory folderUri(String uri) {
    folderUri = uri;
    return this;
  }

  public ElasticQueryFactory user(User user) {
    this.user = user;
    return this;
  }

  /**
   * Build a {@link QueryBuilder} from a {@link SearchQuery}
   *
   * @param query
   * @return
   * @return
   */
  public Query build() {

    final BoolQuery.Builder booleanQuery = new BoolQuery.Builder();
    final Query searchQuery = buildSearchQuery(query, user);
    final Query containerQuery = buildContainerFilter(folderUri, !emptyQuery);
    final Query securityQuery = new SecurityQueryFactory().user(user).searchForCollection(searchForCollection).build();
    final Query statusQuery = buildStatusQuery(query, user);
    final Query filterQuery = buildSearchQuery(query != null ? query.getFilterElements() : new ArrayList<>(), user);

    if (!isMatchAll(searchQuery)) {
      booleanQuery.must(searchQuery);
    }
    if (!isMatchAll(containerQuery)) {
      booleanQuery.must(containerQuery);
    }
    if (!isMatchAll(securityQuery)) {
      booleanQuery.must(securityQuery);
    }
    if (!isMatchAll(filterQuery)) {
      booleanQuery.must(filterQuery);
    }
    if ((emptyQuery && searchForCollection) || !includeSubcollections) {
      booleanQuery.mustNot(ExistsQuery.of(eq -> eq.field(ElasticFields.FOLDER.field()))._toQuery());
    }
    if (!searchForUsers && !isMatchAll(statusQuery)) {
      booleanQuery.must(statusQuery);
    }
    /*
     * parent-child relations use the join_field !!! we need to exclude the content
     * docs
     */
    booleanQuery.mustNot(TermQuery.of(tq -> tq.field(ElasticFields.JOIN_FIELD.field()).value("content"))._toQuery());
    return booleanQuery.build()._toQuery();
  }

  public Query buildBaseQuery() {
    if (query == null || query.isEmpty()) {
      final BoolQuery.Builder booleanQuery = new BoolQuery.Builder();
      final Query containerQuery = buildContainerFilter(folderUri, true);
      final Query securityQuery = new SecurityQueryFactory().user(user).searchForCollection(searchForCollection).build();
      final Query statusQuery = buildStatusQuery(query, user);
      if (!isMatchAll(containerQuery)) {
        booleanQuery.must(containerQuery);
      }
      if (!isMatchAll(securityQuery)) {
        booleanQuery.must(securityQuery);
      }
      if (!searchForUsers && !isMatchAll(statusQuery)) {
        booleanQuery.must(statusQuery);
      }

      return booleanQuery.build()._toQuery();
    }
    return null;
  }

  /**
   * True if the query is a match all query
   *
   * @param q
   * @return
   */
  private boolean isMatchAll(Query q) {
    return q.isMatchAll();
  }

  /**
   * The {@link QueryBuilder} with the search query
   *
   * @param query
   * @return
   */
  private Query buildSearchQuery(SearchQuery query, User user) {
    if (query == null || query.getElements().isEmpty()) {
      return new MatchAllQuery.Builder().build()._toQuery();
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
  private Query buildStatusQuery(SearchQuery query, User user) {
    if (user == null) {
      // Not Logged in: can only view release objects
      return fieldQuery(ElasticFields.STATUS, Status.RELEASED.name(), SearchOperators.EQUALS, false);
    } else if (query != null && (hasStatusQuery(query.getElements()) || hasStatusQuery(query.getFilterElements()))) {
      // Don't filter, since it is done later via the searchquery
      return new MatchAllQuery.Builder().build()._toQuery();
    } else {
      // Default = don't view discarded objects
      return fieldQuery(ElasticFields.STATUS, Status.WITHDRAWN.name(), SearchOperators.EQUALS, true);
    }
  }

  /**
   * Check if at least on {@link SearchPair} is related to the status. If yes, return true
   *
   * @param elements
   * @return
   */
  private boolean hasStatusQuery(List<SearchElement> elements) {
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
  private Query buildSearchQuery(List<SearchElement> elements, User user) {

    if (elements.isEmpty()) {
      return new MatchAllQuery.Builder().build()._toQuery();
    }
    boolean OR = isORSearchGroup(elements);
    final BoolQuery.Builder q = new BoolQuery.Builder();
    for (final SearchElement el : elements) {
      if (el instanceof SearchPair) {
        if (OR) {
          q.should(termQuery((SearchPair) el, user));
        } else {
          q.must(termQuery((SearchPair) el, user));
        }
      } else if (el instanceof SearchLogicalRelation) {
        OR = ((SearchLogicalRelation) el).getLogicalRelation() == LOGICAL_RELATIONS.OR ? true : false;
      } else if (el instanceof SearchGroup) {
        boolean not = ((SearchGroup) el).isNot();
        if (OR) {
          q.should(negate(buildSearchQuery(((SearchGroup) el).getElements(), user), not));
        } else {
          q.must(negate(buildSearchQuery(((SearchGroup) el).getElements(), user), not));
        }
      }
    }
    return q.build()._toQuery();
  }

  private boolean isORSearchGroup(List<SearchElement> elements) {
    for (final SearchElement el : elements) {
      if (el instanceof SearchLogicalRelation) {
        return ((SearchLogicalRelation) el).getLogicalRelation() == LOGICAL_RELATIONS.OR ? true : false;
      }
    }
    return true;
  }

  /**
   * Filter in only elements within the container
   * 
   * @param containerUri
   * @return
   */
  private Query buildContainerFilter(String containerUri, boolean addChildren) {
    if (containerUri == null) {
      return new MatchAllQuery.Builder().build()._toQuery();
    }
    BoolQuery.Builder bq = new BoolQuery.Builder();
    bq.should(fieldQuery(ElasticFields.FOLDER, containerUri, SearchOperators.EQUALS, false));
    if (addChildren) {
      List<String> subCollections = new HierarchyService().findAllSubcollections(containerUri);
      bq.should(TermsQuery.of(tq -> tq.field(ElasticFields.FOLDER.field())
          .terms(t -> t.value(subCollections.stream().map(coll -> FieldValue.of(coll)).collect(Collectors.toList()))))._toQuery());
      /*
       * for (String uri : new HierarchyService().findAllSubcollections(containerUri))
       * { bq.should(fieldQuery(ElasticFields.FOLDER, uri, SearchOperators.EQUALS,
       * false)); }
       */
    }
    return bq.build()._toQuery();
  }

  /**
   * Build the query with all Read grants
   *
   * @param grants
   * @return
   */
  private Query buildGrantQuery(User user, GrantType role) {
    return new SecurityQueryFactory().searchForCollection(searchForCollection).user(user).role(role).build();
  }

  /**
   * Create a QueryBuilder with a term filter (see
   * https://www.elastic.co/guide/en/elasticsearch/reference/current/query-dsl-term-filter.html)
   *
   * @param pair
   * @return
   */
  private Query termQuery(SearchPair pair, User user) {
    if (pair instanceof SearchMetadata) {
      return metadataFilter((SearchMetadata) pair);
    } else if (pair instanceof SearchCollectionMetadata) {
      return collectionMetadataFilter((SearchCollectionMetadata) pair);
    } else if (pair instanceof SearchTechnicalMetadata) {
      return technicalMetadataQuery((SearchTechnicalMetadata) pair);
    }
    final SearchFields index = pair.getField();
    switch (index) {
      case all:
        return allQuery(pair);
      case fulltext:
        return contentQuery(fieldQuery(ElasticFields.FULLTEXT, pair.getValue(), pair.getOperator(), pair.isNot()));
      case checksum:
        return contentQuery(fieldQuery(ElasticFields.CHECKSUM, pair.getValue(), pair.getOperator(), pair.isNot()));
      case col:
        return fieldQuery(ElasticFields.FOLDER, pair.getValue(), pair.getOperator(), pair.isNot());
      case collectionid:
      case collection:
        return fieldQuery(ElasticFields.FOLDER,
            "\"" + ObjectHelper.getURI(CollectionImeji.class, pair.getValue().replaceAll("\"", "")).toString() + "\"", pair.getOperator(),
            pair.isNot());
      case title:
        return fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case description:
        return fieldQuery(ElasticFields.DESCRIPTION, pair.getValue(), pair.getOperator(), pair.isNot());
      case author_familyname:
        return fieldQuery(ElasticFields.AUTHOR_FAMILYNAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case author_givenname:
        return fieldQuery(ElasticFields.AUTHOR_GIVENNAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case author:
        return fieldQuery(ElasticFields.AUTHOR_COMPLETENAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case author_organization:
        return fieldQuery(ElasticFields.AUTHOR_ORGANIZATION, pair.getValue(), pair.getOperator(), pair.isNot());
      case author_organization_exact:
        return fieldQuery(ElasticFields.AUTHOR_ORGANIZATION_EXACT, pair.getValue(), pair.getOperator(), pair.isNot());
      case author_completename_exact:
        return fieldQuery(ElasticFields.AUTHOR_COMPLETENAME_EXACT, pair.getValue(), pair.getOperator(), pair.isNot());
      case collection_type:
        return fieldQuery(ElasticFields.COLLECTION_TYPE, pair.getValue(), pair.getOperator(), pair.isNot());
      case collection_title:
        return parentCollectionQuery(fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot()));
      case collection_description:
        return parentCollectionQuery(fieldQuery(ElasticFields.AUTHOR_COMPLETENAME, pair.getValue(), pair.getOperator(), pair.isNot()));
      case collection_author:
        return fieldQuery(ElasticFields.AUTHORS_OF_COLLECTION, pair.getValue(), pair.getOperator(), pair.isNot());
      case collection_author_organisation:
        return fieldQuery(ElasticFields.ORGANIZATION_OF_COLLECTION, pair.getValue(), pair.getOperator(), pair.isNot());
      case family:
        return fieldQuery(ElasticFields.FAMILYNAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case given:
        return fieldQuery(ElasticFields.GIVENNAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case organization:
        return fieldQuery(ElasticFields.ORGANIZATION, pair.getValue(), pair.getOperator(), pair.isNot());
      case created:
        return timeQuery(ElasticFields.CREATED.field(), pair.getValue(), pair.getOperator(), pair.isNot());
      case creator:
        return fieldQuery(ElasticFields.CREATOR, ElasticSearchFactoryUtil.getUserId(pair.getValue()), pair.getOperator(), pair.isNot());
      case collaborator:
        return collaboratorQuery(pair.getValue());
      case read:
        return fieldQuery(ElasticFields.READ, pair.getValue(), SearchOperators.EQUALS, pair.isNot());
      case filename:
        return fieldQuery(ElasticFields.NAME, pair.getValue(), pair.getOperator(), pair.isNot());
      case filetype:
        return fileTypeQuery(pair);
      case role:
        // same as grant_type
        final GrantType grant = pair.getValue().equals("upload") ? GrantType.EDIT : GrantType.valueOf(pair.getValue().toUpperCase());
        return buildGrantQuery(user, grant);
      case license:
        return licenseQuery(pair);
      case modified:
        return timeQuery(ElasticFields.MODIFIED.field(), pair.getValue(), pair.getOperator(), pair.isNot());
      case status:
        return fieldQuery(ElasticFields.STATUS, formatStatusSearchValue(pair), pair.getOperator(), pair.isNot());
      case pid:
        return fieldQuery(ElasticFields.PID, pair.getValue(), pair.getOperator(), pair.isNot());
      case info_label:
        return fieldQuery(ElasticFields.INFO_LABEL, pair.getValue(), pair.getOperator(), pair.isNot());
      case info_text:
        return fieldQuery(ElasticFields.INFO_TEXT, pair.getValue(), pair.getOperator(), pair.isNot());
      case info_url:
        return fieldQuery(ElasticFields.INFO_URL, pair.getValue(), pair.getOperator(), pair.isNot());
      case email:
        return fieldQuery(ElasticFields.EMAIL, pair.getValue(), SearchOperators.EQUALS, pair.isNot());
      case index:
        return fieldQuery(ElasticFields.METADATA_INDEX, StatementUtil.formatIndex(pair.getValue()), SearchOperators.EQUALS, pair.isNot());
      case completename:
        break;
      case creatorid:
        break;
      case editor:
        break;
      case filesize:
        break;
      case id:
        return fieldQuery(ElasticFields.IDSTRING, pair.getValue(), SearchOperators.EQUALS, pair.isNot());
      case md:
        break;
      case technical:
        break;
      case folder:
        return fieldQuery(ElasticFields.FOLDER, pair.getValue(), pair.getOperator(), pair.isNot());
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
  private Query collectionMetadataFilter(SearchCollectionMetadata md) {
    return collectionMetadataQuery(fieldQuery(ElasticFields.INFO_TEXT, md.getValue(), md.getOperator(), md.isNot()), md.getLabel());
  }



  /**
   * Create a {@link QueryBuilder} for a {@link SearchMetadata}
   *
   * @param md
   * @return
   */
  private Query metadataFilter(SearchMetadata md) {
    if (md.getMetadataField() == null) {
      return metadataQuery(fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
    }
    switch (md.getMetadataField()) {
      case exact:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_EXACT, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case text:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case placename:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_NAME, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case title:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_TITLE, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case number:
        return metadataQuery(numberQuery(ElasticFields.METADATA_NUMBER.field(), md.getValue(), md.getOperator(), md.isNot()),
            md.getIndex());
      case date:
        return metadataQuery(timeQuery(ElasticFields.METADATA_TIME.field(), md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case url:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_URI, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case familyname:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_FAMILYNAME, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case givenname:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_GIVENNAME, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      case coordinates:
        return metadataQuery(geoQuery(md.getValue()), md.getIndex());
      case time:
        return metadataQuery(timeQuery(ElasticFields.METADATA_TIME.field(), md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
      default:
        return metadataQuery(fieldQuery(ElasticFields.METADATA_TEXT, md.getValue(), md.getOperator(), md.isNot()), md.getIndex());
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
  private Query fieldQuery(ElasticFields field, String value, SearchOperators operator, boolean not) {
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
  private Query fieldQuery(String fieldName, String value, SearchOperators operator, boolean not) {
    Query q = null;

    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case EQUALS:
        q = matchFieldQuery(fieldName, value);
        break;
      case GREATER:
      case GREATER_EQUALS:
        q = greaterThanQuery(fieldName, value);
        break;
      case LESSER:
      case LESSER_EQUALS:
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
  private Query timeQuery(String field, String dateString, SearchOperators operator, boolean not) {
    Query q = null;
    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case GREATER:
      case GREATER_EQUALS:
        q = greaterThanQuery(field, Long.toString(DateFormatter.getTime(dateString)));
        break;
      case LESSER:
      case LESSER_EQUALS:
        q = lessThanQuery(field, Long.toString(DateFormatter.getTime(dateString)));
        break;
      default:
        String from = parseFromValue(dateString);
        String to = parseToValue(dateString);
        if (!StringHelper.isNullOrEmptyTrim(from) || !StringHelper.isNullOrEmptyTrim(to)) {
          RangeQuery.Builder rq = new RangeQuery.Builder().field(field);
          if (!StringHelper.isNullOrEmptyTrim(from)) {
            rq.gte(JsonData.of(DateFormatter.getTime(from)));
          }
          if (!StringHelper.isNullOrEmptyTrim(to)) {
            rq.lte(JsonData.of(DateFormatter.getTime(to)));
          }
          q = rq.build()._toQuery();
        } else {
          q = RangeQuery.of(rq -> rq.field(field).gte(JsonData.of(Long.toString(DateFormatter.parseDate(dateString).getTime())))
              .lte(JsonData.of(Long.toString(DateFormatter.parseDate2(dateString).getTime()))))._toQuery();
          // q = QueryBuilders.termQuery(field, DateFormatter.getTime(dateString));
        }
        break;
    }
    return negate(q, not);
  }

  /**
   * Search for a number field
   * 
   * @param field
   * @param number
   * @param operator
   * @param not
   * @return
   */
  private Query numberQuery(String field, String number, SearchOperators operator, boolean not) {
    Query q = null;
    if (operator == null) {
      operator = SearchOperators.EQUALS;
    }
    switch (operator) {
      case GREATER:
      case GREATER_EQUALS:
        q = greaterThanQuery(field, number);
        break;
      case LESSER:
      case LESSER_EQUALS:
        q = lessThanQuery(field, number);
        break;
      default:
        String from = parseFromValue(number);
        String to = parseToValue(number);
        if (!StringHelper.isNullOrEmptyTrim(from) || !StringHelper.isNullOrEmptyTrim(to)) {
          RangeQuery.Builder rq = new RangeQuery.Builder().field(field);
          if (!StringHelper.isNullOrEmptyTrim(from)) {
            rq.gte(JsonData.of(from));
          }
          if (!StringHelper.isNullOrEmptyTrim(to)) {
            rq.lte(JsonData.of(to));
          }
          q = rq.build()._toQuery();
        } else {
          q = TermQuery.of(tq -> tq.field(field).value(number))._toQuery();
        }
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
  private String parseFromValue(String fromToQueryString) {
    int fromIndex = fromToQueryString.indexOf("from");
    int toIndex = fromToQueryString.indexOf("to");
    toIndex = toIndex != -1 ? toIndex : fromToQueryString.length();
    return fromIndex != -1 ? fromToQueryString.substring(fromIndex + "from".length(), toIndex) : null;
  }

  /**
   * Parse the to value in a query with the format: from 123 to 456
   * 
   * @param fromToQueryString
   * @return
   */
  private String parseToValue(String fromToQueryString) {
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
  private Query metadataQuery(Query valueQuery, String statement) {
    return NestedQuery.of(nq -> nq.path(ElasticFields.METADATA.field()).query(BoolQuery
        .of(bq -> bq.must(valueQuery).must(fieldQuery(ElasticFields.METADATA_INDEX, statement, SearchOperators.EQUALS, false)))._toQuery()))
        ._toQuery();
    /*
    return QueryBuilders.nestedQuery(ElasticFields.METADATA.field(),
        QueryBuilders.boolQuery().must(valueQuery).must(fieldQuery(ElasticFields.METADATA_INDEX, statement, SearchOperators.EQUALS, false)),
        ScoreMode.Avg);
    */
  }


  private Query collectionMetadataQuery(Query valueQuery, String statement) {

    return NestedQuery
        .of(nq -> nq.path(ElasticFields.INFO.field()).query(BoolQuery
            .of(bq -> bq.must(valueQuery).must(fieldQuery(ElasticFields.INFO_LABEL, statement, SearchOperators.EQUALS, false)))._toQuery()))
        ._toQuery();

    /*
    return QueryBuilders.nestedQuery(ElasticFields.INFO.field(),
        QueryBuilders.boolQuery().must(valueQuery).must(fieldQuery(ElasticFields.INFO_LABEL, statement, SearchOperators.EQUALS, false)),
        ScoreMode.Avg);
    
     */

  }

  /**
   * Query for technical metadata
   *
   * @param label
   * @param value
   * @param not
   * @return
   */
  private Query technicalMetadataQuery(SearchTechnicalMetadata tmd) {

    return NestedQuery.of(nq -> nq.path(ElasticFields.TECHNICAL.field())
        .query(BoolQuery.of(bq -> bq.must(fieldQuery(ElasticFields.TECHNICAL_NAME, tmd.getLabel(), SearchOperators.EQUALS, false))
            .must(fieldQuery(ElasticFields.TECHNICAL_VALUE, tmd.getValue(), tmd.getOperator(), tmd.isNot())))._toQuery()))
        ._toQuery();

    /*
    return contentQuery(QueryBuilders.nestedQuery(ElasticFields.TECHNICAL.field(),
        QueryBuilders.boolQuery().must(fieldQuery(ElasticFields.TECHNICAL_NAME, tmd.getLabel(), SearchOperators.EQUALS, false))
            .must(fieldQuery(ElasticFields.TECHNICAL_VALUE, tmd.getValue(), tmd.getOperator(), tmd.isNot())),
        ScoreMode.Avg));
    
     */
  }

  /**
   * Search for a match (not the exact value)
   *
   * @param field
   * @param value
   * @return
   */
  private Query matchFieldQuery(String fieldName, String value) {

    if (ElasticFields.ALL.field().equalsIgnoreCase(fieldName)) {
      return QueryStringQuery.of(qs -> qs.query(fieldName + ":" + value + " " + ElasticFields.NAME.field() + ".suggest:" + value))
          ._toQuery();
      //return QueryBuilders.queryStringQuery(fieldName + ":" + value + " " + ElasticFields.NAME.field() + ".suggest:" + value);
    }

    return QueryStringQuery.of(qs -> qs.query(fieldName + ":" + value))._toQuery();
    //return QueryBuilders.queryStringQuery(fieldName + ":" + value);
  }

  /**
   * Search for value greater than the searched value
   *
   * @param field
   * @param value
   * @return
   */
  private Query greaterThanQuery(String fieldName, String value) {
    if (NumberUtils.isNumber(value)) {
      return RangeQuery.of(rq -> rq.field(fieldName).gte(JsonData.of(Double.parseDouble(value))))._toQuery();
      //return QueryBuilders.rangeQuery(fieldName).gte(Double.parseDouble(value));
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
  private Query lessThanQuery(String fieldName, String value) {
    if (NumberUtils.isNumber(value)) {
      return RangeQuery.of(rq -> rq.field(fieldName).lte(JsonData.of(Double.parseDouble(value))))._toQuery();
      //return QueryBuilders.rangeQuery(fieldName).lte(Double.parseDouble(value));
    }
    return matchNothing();
  }

  private Query geoQuery(String value) {
    final String[] values = value.split(",");
    String distance = null;
    final double lat = Double.parseDouble(values[0]);
    final double lon = Double.parseDouble(values[1]);
    if (values.length == 3) {
      distance = (values[2].equals("0") || values[2].matches("[0]+[a-zA-Z]{1,2}$")) ? distance : values[2];
    } else {
      distance = "1cm";
    }

    final String dist = distance;
    return GeoDistanceQuery.of(gd -> gd.field(ElasticFields.METADATA_LOCATION.field()).distance(dist)
        .location(gl -> gl.latlon(latLon -> latLon.lat(lat).lon(lon))))._toQuery();

    //return QueryBuilders.geoDistanceQuery(ElasticFields.METADATA_LOCATION.field()).distance(distance).point(lat, lon);
  }

  /**
   * Add NOT filter to the {@link Filter} if not is true
   *
   * @param f
   * @param not
   * @return
   */
  private Query negate(Query f, boolean not) {
    return not ? BoolQuery.of(bq -> bq.mustNot(f))._toQuery() : f;
  }

  /**
   * Negate the query
   * 
   * @param f
   * @return
   */
  private Query negate(Query f) {
    return negate(f, true);
  }

  /**
   * Return a query which find nothing
   *
   * @return
   */
  private Query matchNothing() {
    return BoolQuery.of(bq -> bq.mustNot(new MatchAllQuery.Builder().build()._toQuery()))._toQuery();
  }

  /**
   * Search for items/collections of collaobrator, i.e. users with who the collections have been
   * shared
   * 
   * @param email
   * @return
   */
  private Query collaboratorQuery(String email) {
    try {
      User user = new UserService().retrieve(email, Imeji.adminUser);
      Query creatorQuery = fieldQuery(ElasticFields.CREATOR, user.getId().toString(), SearchOperators.EQUALS, false);
      Query roleQuery = new SecurityQueryFactory().user(user).searchForCollection(searchForCollection).role(GrantType.READ).build();
      return BoolQuery.of(bq -> bq.must(roleQuery).mustNot(creatorQuery))._toQuery();
    } catch (Exception e) {
      LOGGER.error("Error building collaborator query", e);
      return matchNothing();
    }
  }

  /**
   * Build Query for all terms
   * 
   * @param pair
   * @return
   */
  private Query allQuery(SearchPair pair) {
    final BoolQuery.Builder f =
        new BoolQuery.Builder().should(fieldQuery(ElasticFields.ALL, pair.getValue(), SearchOperators.EQUALS, false));
    if (NumberUtils.isNumber(pair.getValue())) {
      f.should(fieldQuery(ElasticFields.METADATA_NUMBER, pair.getValue(), SearchOperators.EQUALS, false));
    }
    return negate(f.build()._toQuery(), pair.isNot());
  }

  /**
   * Search for content
   * 
   * @param q
   * @return
   */
  private Query contentQuery(Query q) {
    return HasChildQuery.of(hcq -> hcq.type("content").query(q).scoreMode(ChildScoreMode.None))._toQuery();
    //return JoinQueryBuilders.hasChildQuery("content", q, ScoreMode.None);
  }

  /**
   * Make a query for license
   * 
   * @param pair
   * @return
   */
  private Query licenseQuery(SearchPair pair) {
    final BoolQuery.Builder licenseQuery = new BoolQuery.Builder();
    for (final String licenseName : pair.getValue().split(" OR ")) {
      if ("*".equals(licenseName)) {
        licenseQuery.mustNot(fieldQuery(ElasticFields.LICENSE, ImejiLicenses.NO_LICENSE, SearchOperators.EQUALS, false));
        licenseQuery.should(ExistsQuery.of(eq -> eq.field(ElasticFields.LICENSE.field()))._toQuery());
      } else {
        licenseQuery.should(fieldQuery(ElasticFields.LICENSE, licenseName, SearchOperators.EQUALS, false));
      }
    }
    return licenseQuery.build()._toQuery();
  }

  private Query parentCollectionQuery(Query qb) {
    return HasParentQuery.of(hpq -> hpq.parentType(ElasticIndices.folders.name()).query(qb).ignoreUnmapped(emptyQuery))._toQuery();
    //return JoinQueryBuilders.hasParentQuery(ElasticIndices.folders.name(), qb, emptyQuery);
  }

  /**
   * Build a query to search for filetypes
   * 
   * @param pair
   * @return
   */
  private Query fileTypeQuery(SearchPair pair) {
    final BoolQuery.Builder filetypeQuery = new BoolQuery.Builder();
    for (final String ext : SearchUtils.parseFileTypesAsExtensionList(pair.getValue().replace("\"", ""))) {

      filetypeQuery.should(fieldQuery(ElasticFields.NAME.field() + ".suggest", "*." + ext + "", SearchOperators.EQUALS, false));
    }
    return filetypeQuery.build()._toQuery();

  }

  /**
   * Format the search value for the status, as indexed
   * 
   * @param pair
   * @return
   */
  private String formatStatusSearchValue(SearchPair pair) {
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

  public boolean isIncludeSubcollections() {
    return includeSubcollections;
  }

  public void setIncludeSubcollections(boolean includeSubcollections) {
    this.includeSubcollections = includeSubcollections;
  }
}
