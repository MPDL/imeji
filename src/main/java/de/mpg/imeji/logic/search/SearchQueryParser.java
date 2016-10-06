/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchSimpleMetadata;
import de.mpg.imeji.logic.search.model.SearchTechnicalMetadata;
import de.mpg.imeji.logic.search.util.StringParser;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Statement;

/**
 * Static methods to manipulate imeji url search queries
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SearchQueryParser {
  /**
   * Regex to match: statementid:field="value"
   */
  private static final String SEARCH_METADATA_REGEX =
      "([a-zA-Z0-9-_]+):([a-z_]+)([=<>@]{1,2})\"(.+)\"";
  /**
   * Regex to match: field="value"
   */
  private static final String SEARCH_PAIR_REGEX = "([a-zA-Z_]+)([=<>@]{1,2})\"(.+)\"";
  /**
   * Search for technical metadata technical:index="value"
   */
  private static String SEARCH_TECHNICAL_METADATA_REGEX =
      "technical\\[(.+)\\]([=<>@]{1,2})\"(.+)\"";
  /**
   * Regex to match md:label="value"
   */
  private static String SEARCH_METADATA_SIMPLE_REGEX = "metadata\\[(.+)\\]([=<>@]{1,2})\"(.+)\"";
  /**
   * PAttern for SEARCH_METADATA_REGEX
   */
  private static final Pattern SEARCH_METADATA_PATTERN = Pattern.compile(SEARCH_METADATA_REGEX);
  /**
   * Pattern for SEARCH_PAIR_REGEX
   */
  private static final Pattern SEARCH_PAIR_PATTERN = Pattern.compile(SEARCH_PAIR_REGEX);
  /**
   * Pattern for SEARCH_METADATA_SIMPLE_REGEX
   */
  private static Pattern SEARCH_METADATA_SIMPLE_PATTERN =
      Pattern.compile(SEARCH_METADATA_SIMPLE_REGEX);

  /**
   * Pattern for SEARCH_TECHNICAL_METADATA_REGEX
   */
  private static Pattern SEARCH_TECHNICAL_METADATA_PATTERN =
      Pattern.compile(SEARCH_TECHNICAL_METADATA_REGEX);

  /**
   * Private Constructor
   */
  private SearchQueryParser() {
    // Avoid creation
  }

  /**
   * Parse a url search query into a {@link SearchQuery}. Decode the query with UTF-8
   *
   * @param query
   * @return
   * @throws UnprocessableError
   * @throws IOException
   */
  public static SearchQuery parseStringQuery(String query) throws UnprocessableError {
    if (query == null) {
      query = "";
    }
    String decodedQuery;
    try {
      decodedQuery = URLDecoder.decode(query, "UTF-8");
      return parseStringQueryDecoded(decodedQuery);
    } catch (IOException e) {
      throw new UnprocessableError("Query could not be parsed: " + query);
    }
  }

  /**
   * Parse a url search query into a {@link SearchQuery}. The query should be already decoded
   *
   * @param query
   * @return
   * @throws IOException
   * @throws UnprocessableError
   */
  public static SearchQuery parseStringQueryDecoded(String query) throws UnprocessableError {
    SearchQuery searchQuery = new SearchQuery();
    String subQuery = "";
    String scString = "";
    boolean not = false;
    boolean hasBracket = false; // don't try to look for group if there
    // isn't any bracket
    int bracketsOpened = 0;
    int bracketsClosed = 0;
    if (query == null) {
      query = "";
    }
    StringReader reader = new StringReader(query);
    int c = 0;
    StringParser simpleMdParser = new StringParser(SEARCH_METADATA_SIMPLE_PATTERN);
    StringParser mdParser = new StringParser(SEARCH_METADATA_PATTERN);
    StringParser pairParser = new StringParser(SEARCH_PAIR_PATTERN);
    StringParser technicalMdParser = new StringParser(SEARCH_TECHNICAL_METADATA_PATTERN);
    try {
      while ((c = reader.read()) != -1) {
        if (bracketsOpened - bracketsClosed != 0) {
          subQuery += (char) c;
        } else {
          scString += (char) c;
        }
        if (c == '(') {
          hasBracket = true;
          bracketsOpened++;
        }
        if (c == ')') {
          bracketsClosed++;
          scString = "";
        }
        if (scString.toUpperCase().trim().equals("AND")
            || scString.toUpperCase().trim().equals("OR")) {
          searchQuery.getElements().add(
              new SearchLogicalRelation(LOGICAL_RELATIONS.valueOf(scString.toUpperCase().trim())));
          scString = "";
        }
        if (scString.toUpperCase().trim().equals("NOT")) {
          not = true;
          scString = "";
        }
        if (hasBracket && (bracketsOpened - bracketsClosed == 0)) {
          SearchQuery subSearchQuery = parseStringQueryDecoded(subQuery);
          if (!subSearchQuery.isEmpty()) {
            SearchGroup searchGroup = new SearchGroup();
            searchGroup.getGroup().addAll(parseStringQueryDecoded(subQuery).getElements());
            searchQuery.getElements().add(searchGroup);
            subQuery = "";
          }
        }
        if (simpleMdParser.find(scString)) {
          searchQuery.addPair(new SearchSimpleMetadata(simpleMdParser.getGroup(1),
              stringOperator2SearchOperator(simpleMdParser.getGroup(2)), simpleMdParser.getGroup(3),
              not));
          not = false;
          scString = "";
        } else if (technicalMdParser.find(scString)) {
          SearchOperators operator = stringOperator2SearchOperator(technicalMdParser.getGroup(2));
          String label = technicalMdParser.getGroup(1);
          String value = technicalMdParser.getGroup(3);
          searchQuery.addPair(
              new SearchTechnicalMetadata(SearchFields.technical, operator, value, label, not));
          not = false;
          scString = "";
        } else if (mdParser.find(scString)) {
          SearchOperators operator = stringOperator2SearchOperator(mdParser.getGroup(3));
          String value = mdParser.getGroup(4);
          value = value.startsWith("\"") ? value + "\"" : value;
          SearchFields field = SearchFields.valueOf(mdParser.getGroup(2));
          URI statementId = ObjectHelper.getURI(Statement.class, mdParser.getGroup(1));
          searchQuery.addPair(new SearchMetadata(field, operator, value, statementId, not));
          not = false;
          scString = "";
        } else if (pairParser.find(scString)) {
          SearchOperators operator = stringOperator2SearchOperator(pairParser.getGroup(2));
          SearchFields field = SearchFields.valueOf(pairParser.getGroup(1));
          String value = pairParser.getGroup(3);
          value = value.startsWith("\"") ? value + "\"" : value;
          searchQuery.addPair(new SearchPair(field, operator, value, not));
          scString = "";
          not = false;
        }
      }
    } catch (IOException e) {
      throw new UnprocessableError(e);
    }
    if (!"".equals(query) && searchQuery.isEmpty()) {
      searchQuery
          .addPair(new SearchPair(SearchFields.all, SearchOperators.REGEX, query.trim(), false));
    }
    return searchQuery;
  }

  /**
   * Transform a {@link String} to a {@link SearchOperators}
   *
   * @param str
   * @return
   */
  private static SearchOperators stringOperator2SearchOperator(String str) {
    if ("=".equals(str)) {
      return SearchOperators.REGEX;
    } else if ("==".equals(str)) {
      return SearchOperators.EQUALS;
    } else if (">=".equals(str)) {
      return SearchOperators.GREATER;
    } else if ("<=".equals(str)) {
      return SearchOperators.LESSER;
    } else if ("@".equals(str)) {
      return SearchOperators.GEO;
    }
    return SearchOperators.REGEX;
  }

  /**
   * True is a {@link SearchQuery} is a simple search (i.e. triggered from the simple search form)
   *
   * @param searchQuery
   * @return
   */
  public static boolean isSimpleSearch(SearchQuery searchQuery) {
    for (SearchElement element : searchQuery.getElements()) {
      if (SEARCH_ELEMENTS.PAIR.equals(element.getType())
          && ((SearchPair) element).getField() == SearchFields.all) {
        return true;
      }
    }
    return false;
  }

  /**
   * Transform a {@link SearchQuery} into a url search query encorded in UTF-8
   *
   * @param searchQuery
   * @return
   */
  public static String transform2UTF8URL(SearchQuery searchQuery) {
    try {
      return URLEncoder.encode(transform2URL(searchQuery), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      throw new RuntimeException("Error encoding search query: " + searchQuery, e);
    }
  }

  /**
   * Transform a {@link SearchQuery} into a url search query
   *
   * @param searchQuery
   * @return
   */
  public static String transform2URL(SearchQuery searchQuery) {
    String query = "";
    String logical = "";
    for (SearchElement se : searchQuery.getElements()) {
      switch (se.getType()) {
        case GROUP:
          if (((SearchGroup) se).isNot()) {
            query += " NOT";
          }
          String g = transform2URL(new SearchQuery(((SearchGroup) se).getGroup()));
          if (!"".equals(g)) {
            query += logical + "(" + g + ")";
          }
          break;
        case LOGICAL_RELATIONS:
          logical = " " + ((SearchLogicalRelation) se).getLogicalRelation().name() + " ";
          break;
        case PAIR:
          if ("".equals(((SearchPair) se).getValue())) {
            break;
          }
          if (((SearchPair) se).isNot()) {
            query += " NOT";
          }
          query += logical + ((SearchPair) se).getField()
              + operator2URL(((SearchPair) se).getOperator()) + searchValue2URL(((SearchPair) se));
          break;
        case METADATA:
          if (((SearchMetadata) se).isNot()) {
            query += " NOT";
          }
          query += logical
              + transformStatementToIndex(((SearchMetadata) se).getStatement(),
                  ((SearchPair) se).getField())
              + operator2URL(((SearchMetadata) se).getOperator())
              + searchValue2URL(((SearchMetadata) se));
          break;
        case TECHNICAL_METADATA:
          if (((SearchTechnicalMetadata) se).isNot()) {
            query += " NOT";
          }
          query +=
              logical + SearchFields.technical + "[" + ((SearchTechnicalMetadata) se).getLabel()
                  + "]=\"" + ((SearchTechnicalMetadata) se).getValue() + "\"";
          break;
        default:
          break;
      }
    }
    return query.trim();
  }

  /**
   * Transform a {@link Statement} to an index
   *
   * @param statement
   * @param index
   * @return
   */
  public static String transformStatementToIndex(URI statement, SearchFields field) {
    return ObjectHelper.getId(statement) + ":" + field;
  }

  /**
   * REturn the search value of the {@link SearchMetadata} as string for an url
   *
   * @param md
   * @return
   */
  private static String searchValue2URL(SearchPair pair) {
    return "\"" + pair.getValue() + "\"";
  }

  /**
   * Transform a {@link SearchOperators} to a {@link String} value used in url query
   *
   * @param op
   * @return
   */
  private static String operator2URL(SearchOperators op) {
    switch (op) {
      case GREATER:
        return ">=";
      case LESSER:
        return "<=";
      case REGEX:
        return "=";
      case GEO:
        return "@";
      default:
        return "==";
    }
  }

  /**
   * Transform a {@link SearchQuery} into a user friendly query
   *
   * @param sq
   * @return
   */
  public static String searchQuery2PrettyQuery(SearchQuery sq, Locale locale,
      Map<URI, String> metadataLabelMap) {
    if (sq == null) {
      return "";
    }
    return searchElements2PrettyQuery(sq.getElements(), locale, metadataLabelMap);
  }

  /**
   * Transform a {@link SearchPair} into a user friendly query
   *
   * @param pair
   * @return
   */
  private static String searchPair2PrettyQuery(SearchPair pair) {
    if (pair == null || pair.getField() == null || pair.getValue() == null
        || pair.getValue().equals("")) {
      return "";
    }
    if (pair.getField() == SearchFields.all) {
      return pair.getValue();
    } else {
      return indexNamespace2PrettyQuery(pair.getField().name()) + " "
          + negation2PrettyQuery(pair.isNot()) + searchOperator2PrettyQuery(pair.getOperator())
          + " " + pair.getValue();
    }
  }

  /**
   * Transform a {@link SearchGroup} into a user friendly query
   *
   * @param group
   * @return
   */
  private static String searchGroup2PrettyQuery(SearchGroup group, Locale locale,
      Map<URI, String> metadataLabelMap) {
    String str = "";
    int groupSize = group.getElements().size();
    if (isSearchGroupForComplexMetadata(group)) {
      for (SearchElement md : group.getElements()) {
        if (md instanceof SearchMetadata) {
          str += searchMetadata2PrettyQuery((SearchMetadata) md, locale, metadataLabelMap);
        } else if (md instanceof SearchLogicalRelation) {
          str += searchLogicalRelation2PrettyQuery((SearchLogicalRelation) md, locale);
        }
      }
      // str =
      // searchMetadata2PrettyQuery((SearchMetadata)group.getElements().get(0));
      // groupSize = 1;
    } else {
      str = searchElements2PrettyQuery(group.getElements(), locale, metadataLabelMap);
    }
    if ("".equals(str)) {
      return "";
    }
    if (groupSize > 1) {
      return " (" + removeUseLessLogicalOperation(str, locale) + ") ";
    } else {
      return removeUseLessLogicalOperation(str, locale);
    }
  }

  /**
   * Check if the search group is an group with pair about the same metadata. For instance, when
   * searching for person, the search group will be conposed of many pairs (family-name, givennane,
   * etc) which sould be displayed as a pretty query of only one metadata (person = value)
   *
   * @param group
   * @return
   */
  private static boolean isSearchGroupForComplexMetadata(SearchGroup group) {
    List<String> statementUris = new ArrayList<String>();
    for (SearchElement el : group.getElements()) {
      if (el.getType().equals(SEARCH_ELEMENTS.METADATA)) {
        SearchMetadata md = (SearchMetadata) el;
        if (statementUris.contains(md.getStatement().toString())) {
          return true;
        }
        statementUris.add(md.getStatement().toString());
      }
    }
    return false;
  }

  /**
   * transform a {@link SearchLogicalRelation} into a user friendly query
   *
   * @param rel
   * @return
   */
  private static String searchLogicalRelation2PrettyQuery(SearchLogicalRelation rel,
      Locale locale) {
    switch (rel.getLogicalRelation()) {
      case AND:
        return " " + Imeji.RESOURCE_BUNDLE.getLabel("and_big", locale) + " ";
      default:
        return " " + Imeji.RESOURCE_BUNDLE.getLabel("or_big", locale) + " ";
    }
  }

  /**
   * transform a {@link SearchTechnicalMetadata} into a user friendly query
   *
   * @param rel
   * @return
   */
  private static String searchLTechnicalMetadata2PrettyQuery(SearchTechnicalMetadata tmd,
      Locale locale) {
    return tmd.getLabel() + "=" + tmd.getValue();
  }

  /**
   * Transform a {@link SearchElement} into a user friendly query
   *
   * @param els
   * @return
   */
  private static String searchElements2PrettyQuery(List<SearchElement> els, Locale locale,
      Map<URI, String> metadataLabelMap) {
    String q = "";
    for (SearchElement el : els) {
      switch (el.getType()) {
        case PAIR:
          q += searchPair2PrettyQuery((SearchPair) el);
          break;
        case GROUP:
          q += searchGroup2PrettyQuery((SearchGroup) el, locale, metadataLabelMap);
          break;
        case LOGICAL_RELATIONS:
          q += searchLogicalRelation2PrettyQuery((SearchLogicalRelation) el, locale);
          break;
        case METADATA:
          q += searchMetadata2PrettyQuery((SearchMetadata) el, locale, metadataLabelMap);
          break;
        case TECHNICAL_METADATA:
          q += searchLTechnicalMetadata2PrettyQuery((SearchTechnicalMetadata) el, locale);
          break;
        default:
          break;
      }
    }
    return removeUseLessLogicalOperation(q.trim(), locale);
  }

  /**
   * Remove a logical operation if is not followed by a non empty search element
   *
   * @param q
   * @return
   */
  private static String removeUseLessLogicalOperation(String q, Locale locale) {
    String orString = Imeji.RESOURCE_BUNDLE.getLabel("or_big", locale);
    String andString = Imeji.RESOURCE_BUNDLE.getLabel("and_big", locale);
    if (q.endsWith(" ")) {
      q = q.substring(0, q.length() - 1);
    }
    if (q.endsWith(" " + andString)) {
      q = q.substring(0, q.length() - andString.length());
    }
    if (q.endsWith(" " + orString)) {
      q = q.substring(0, q.length() - orString.length());
    }
    if (q.startsWith(orString)) {
      q = q.substring(orString.length(), q.length());
    }
    if (q.startsWith(andString)) {
      q = q.substring(andString.length(), q.length());
    }
    if (q.endsWith(" ") || q.endsWith(" " + Imeji.RESOURCE_BUNDLE.getLabel("and_big", locale))
        || q.endsWith(" " + Imeji.RESOURCE_BUNDLE.getLabel("or_big", locale))) {
      q = removeUseLessLogicalOperation(q, locale);
    }
    return q.trim();
  }

  /**
   * transform a namespace of a {@link SearchIndex} into a user friendly value
   *
   * @param namespace
   * @return
   */
  public static String indexNamespace2PrettyQuery(String namespace) {
    String s[] = namespace.split("/");
    if (s.length > 0) {
      return namespace.split("/")[s.length - 1];
    }
    return namespace;
  }

  /**
   * Transform a {@link SearchOperators} into a user friendly label
   *
   * @param op
   * @return
   */
  private static String searchOperator2PrettyQuery(SearchOperators op) {
    switch (op) {
      case GREATER:
        return ">=";
      case LESSER:
        return "<=";
      case EQUALS:
        return "==";
      default:
        return "=";
    }
  }

  /**
   * Display a negation in a user friendly way
   *
   * @param isNot
   * @return
   */
  private static String negation2PrettyQuery(boolean isNot) {
    if (isNot) {
      return "!";
    }
    return "";
  }

  /**
   * Special case to display a search for a metadata in a
   *
   * @param group
   * @return
   */
  private static String searchMetadata2PrettyQuery(SearchMetadata md, Locale locale,
      Map<URI, String> metadataLabelMap) {
    String label = metadataLabelMap.get(md.getStatement());
    if (label == null) {
      label = "Metadata-" + indexNamespace2PrettyQuery(md.getStatement().toString());
    }
    switch (md.getField()) {
      case coordinates:
        label += "(" + Imeji.RESOURCE_BUNDLE.getLabel("geolocation_location", locale) + ")";
        break;
      case person_family:
        label += "(" + Imeji.RESOURCE_BUNDLE.getLabel("family_name", locale) + ")";
        break;
      case person_given:
        label += "(" + Imeji.RESOURCE_BUNDLE.getLabel("first_name", locale) + ")";
        break;
      case person_id:
        label += "( " + Imeji.RESOURCE_BUNDLE.getLabel("identifier", locale) + ")";
        break;
      case person_org_name:
        label += "(" + Imeji.RESOURCE_BUNDLE.getLabel("organization", locale) + ")";
        break;
      case url:
        label += "(" + Imeji.RESOURCE_BUNDLE.getLabel("url", locale) + ")";
        break;
      default:
        break;
    }
    return label + " " + negation2PrettyQuery(md.isNot())
        + searchOperator2PrettyQuery(md.getOperator()) + " " + md.getValue();
  }
}
