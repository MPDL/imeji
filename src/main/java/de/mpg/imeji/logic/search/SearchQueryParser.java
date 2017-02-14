package de.mpg.imeji.logic.search;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchTechnicalMetadata;
import de.mpg.imeji.logic.search.util.StringParser;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.TechnicalMetadata;

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
      SearchFields.md.name() + "\\.([a-zA-Z0-9-_]+)(\\.([a-z_]+)){0,1}([=<>@]{1,2})\"(.+)\"";
  /**
   * Regex to match: field="value"
   */
  private static final String SEARCH_PAIR_REGEX = "([a-zA-Z_]+)([=<>@]{1,2})\"(.+)\"";


  /**
   * Search for technical metadata technical:index="value"
   */
  private static final String SEARCH_TECHNICAL_METADATA_REGEX =
      "technical\\[(.+)\\]([=<>@]{1,2})\"(.+)\"";
  /**
   * PAttern for SEARCH_METADATA_REGEX
   */
  private static final Pattern SEARCH_METADATA_PATTERN = Pattern.compile(SEARCH_METADATA_REGEX);
  /**
   * Pattern for SEARCH_PAIR_REGEX
   */
  private static final Pattern SEARCH_PAIR_PATTERN = Pattern.compile(SEARCH_PAIR_REGEX);

  /**
   * Pattern for SEARCH_TECHNICAL_METADATA_REGEX
   */
  private final static Pattern SEARCH_TECHNICAL_METADATA_PATTERN =
      Pattern.compile(SEARCH_TECHNICAL_METADATA_REGEX);



  private static String PAIR_REGEX = "([a-zA-Z0-9-_\\.]+)([=<>]{1,2})(.+)";
  private static Pattern PAIR_PATTERN = Pattern.compile(PAIR_REGEX);
  private static String METADATA_REGEX =
      SearchFields.md.name() + "\\.([a-zA-Z0-9-_\\.]+)([=<>]{1,2})(.+)";
  private static Pattern METADATA_PATTERN = Pattern.compile(METADATA_REGEX);
  private static final String TECHNICAL_REGEX =
      SearchFields.technical.name() + "\\[(.+)\\]([=<>@]{1,2})\"(.+)\"";
  private static Pattern TECHNICAL_PATTERN = Pattern.compile(TECHNICAL_REGEX);


  /**
   * Private Constructor
   */
  private SearchQueryParser() {

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
      return parsedecoded(decodedQuery);
    } catch (final IOException e) {
      throw new UnprocessableError("Query could not be parsed: " + query);
    }
  }


  public static void main(String[] args) throws UnprocessableError {
    String q =
        "author=bas OR (title=this is an example AND (description=\"Super Description\" OR created=2000) OR (md.description=\"Other Description\" OR (md.created.number>=2000 AND md.created.number<50)))";
    SearchQuery sq = parsedecoded(q);
    System.out.println(q);
    System.out.println(transform2URL(sq));
    q = "(md.title.text=hi OR md.description.text=Bob OR md.date.text=2017-02-08)";
    sq = parsedecoded(q);
    System.out.println(q);
    System.out.println(transform2URL(sq));
    q = "md.location.coordinates=5.0,7.0";
    sq = parsedecoded(q);
    System.out.println(q);
    System.out.println(transform2URL(sq));
  }

  public static SearchQuery parsedecoded(final String query) throws UnprocessableError {
    return new SearchFactory().addElement(parseGroup(query), LOGICAL_RELATIONS.AND).build();
  }

  private static SearchGroup parseGroup(String group) throws UnprocessableError {
    final StringReader reader = new StringReader(group);
    final SearchFactory factory = new SearchFactory();
    int c = 0;
    String part = "";
    int brackets = 0;
    boolean inBracket = false;
    LOGICAL_RELATIONS relation = LOGICAL_RELATIONS.AND;
    try {
      while ((c = reader.read()) != -1) {
        part += (char) c;
        if (c == '(') {
          brackets++;
          inBracket = true;
        } else if (c == ')') {
          brackets--;
        }
        if (inBracket && brackets == 0) {
          factory.addElement(parseGroup(part.substring(1, part.length())), relation);
          part = "";
          inBracket = false;
        } else if (!inBracket && endsWithStopWord(part)) {
          factory.addElement(parsePair(removeStopWord(part)), relation);
          relation = readRelation(part);
          part = "";
        }
      }
      if (!StringHelper.isNullOrEmptyTrim(part)) {
        factory.addElement(parsePair(removeStopWord(part)), relation);
      }
    } catch (Exception e) {
      throw new UnprocessableError(e);
    }
    return factory.buildAsGroup();
  }

  /**
   * Parse a string as a pair: index=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parsePair(String s) {
    if (new StringParser(METADATA_PATTERN).find(s)) {
      return parseMetadata(s);
    } else if (new StringParser(TECHNICAL_PATTERN).find(s)) {
      return parseTechnical(s);
    } else {
      return parseSearchPair(s);
    }
  }

  /**
   * Parse to a {@link SearchPair} following string: index=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseSearchPair(String s) {
    StringParser parser = new StringParser(PAIR_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      return new SearchPair(SearchFields.valueOf(index), operator, value, false);
    }
    return new SearchPair();
  }

  /**
   * Parse to a {@link SearchMetadata} following strings: md.statement.index=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseMetadata(String s) {
    StringParser parser = new StringParser(METADATA_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      String[] indexes = index.split("\\.");
      return new SearchMetadata(indexes[0],
          indexes.length > 1 ? SearchFields.valueOf(indexes[1]) : null, operator, value, false);
    }
    return new SearchPair();
  }

  /**
   * Parse to a {@link TechnicalMetadata} following strings: technical[label]=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseTechnical(String s) {
    StringParser parser = new StringParser(TECHNICAL_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      return new SearchTechnicalMetadata(operator, value, index, false);
    }
    return new SearchPair();
  }

  /**
   * True if the String is equals to AND of OR
   * 
   * @param s
   * @return
   */
  private static LOGICAL_RELATIONS readRelation(String s) {
    return s.endsWith("OR") ? LOGICAL_RELATIONS.OR : LOGICAL_RELATIONS.AND;
  }

  /**
   * Remove the stop words from this string
   * 
   * @param s
   * @return
   */
  private static String removeStopWord(String s) {
    return s.replace("AND", "").replace("OR", "").replace(")", "").trim();
  }

  /**
   * True if the String is ended with a Search Query Stop word.
   * 
   * @param s
   * @return
   */
  private static boolean endsWithStopWord(String s) {
    return s.endsWith("AND") || s.endsWith("OR") || s.endsWith(")");
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
    final SearchQuery searchQuery = new SearchQuery();
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
    final StringReader reader = new StringReader(query);
    int c = 0;
    final StringParser mdParser = new StringParser(SEARCH_METADATA_PATTERN);
    final StringParser pairParser = new StringParser(SEARCH_PAIR_PATTERN);
    final StringParser technicalMdParser = new StringParser(SEARCH_TECHNICAL_METADATA_PATTERN);
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
          final SearchQuery subSearchQuery = parseStringQueryDecoded(subQuery);
          if (!subSearchQuery.isEmpty()) {
            final SearchGroup searchGroup = new SearchGroup();
            searchGroup.getGroup().addAll(parseStringQueryDecoded(subQuery).getElements());
            searchQuery.getElements().add(searchGroup);
            subQuery = "";
          }
        }
        if (technicalMdParser.find(scString)) {
          final SearchOperators operator =
              stringOperator2SearchOperator(technicalMdParser.getGroup(2));
          final String label = technicalMdParser.getGroup(1);
          final String value = technicalMdParser.getGroup(3);
          searchQuery.addPair(new SearchTechnicalMetadata(operator, value, label, not));
          not = false;
          scString = "";
        } else if (mdParser.find(scString)) {
          final String index = mdParser.getGroup(1);
          final SearchOperators operator = stringOperator2SearchOperator(mdParser.getGroup(3));
          String value = mdParser.getGroup(4);
          if (value.startsWith("\"")) {
            reader.read();
            value = value + "\"";
          }
          final SearchFields field =
              mdParser.getGroup(2) != null ? SearchFields.valueOf(mdParser.getGroup(2)) : null;
          searchQuery.addPair(new SearchMetadata(index, field, operator, value, not));
          not = false;
          scString = "";
        } else if (pairParser.find(scString)) {
          final SearchOperators operator = stringOperator2SearchOperator(pairParser.getGroup(2));
          final SearchFields field = SearchFields.valueOf(pairParser.getGroup(1));
          String value = pairParser.getGroup(3);
          if (value.startsWith("\"")) {
            reader.read();
            value = value + "\"";
          }
          searchQuery.addPair(new SearchPair(field, operator, value, not));
          scString = "";
          not = false;
        }
      }
    } catch (final IOException e) {
      throw new UnprocessableError(e);
    }
    if (!"".equals(query) && searchQuery.isEmpty()) {
      searchQuery
          .addPair(new SearchPair(SearchFields.all, SearchOperators.EQUALS, query.trim(), false));
      searchQuery.addLogicalRelation(LOGICAL_RELATIONS.OR);
      searchQuery.addPair(
          new SearchPair(SearchFields.fulltext, SearchOperators.EQUALS, query.trim(), false));
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
      return SearchOperators.EQUALS;
    } else if (">".equals(str)) {
      return SearchOperators.GREATER;
    } else if ("<".equals(str)) {
      return SearchOperators.LESSER;
    } else if (">=".equals(str)) {
      return SearchOperators.GREATER_EQUALS;
    } else if ("<=".equals(str)) {
      return SearchOperators.LESSER_EQUALS;
    }
    return SearchOperators.EQUALS;
  }

  /**
   * True is a {@link SearchQuery} is a simple search (i.e. triggered from the simple search form)
   *
   * @param searchQuery
   * @return
   */
  public static boolean isSimpleSearch(SearchQuery searchQuery) {
    for (final SearchElement element : searchQuery.getElements()) {
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
    } catch (final UnsupportedEncodingException e) {
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
    for (final SearchElement se : searchQuery.getElements()) {
      query = query.isEmpty() ? query : query + " ";
      switch (se.getType()) {
        case GROUP:
          query += searchGroupToStringQuery((SearchGroup) se);
          break;
        case LOGICAL_RELATIONS:
          query += ((SearchLogicalRelation) se).getLogicalRelation().name();
          break;
        case PAIR:
          query += searchPairToStringQuery((SearchPair) se);
          break;
        case METADATA:
          query += searchMetadataToStringQuery((SearchMetadata) se);
          break;
        case TECHNICAL_METADATA:
          query += searchTechnicalMetadataToStringQuery((SearchTechnicalMetadata) se);
        default:
          break;
      }
    }
    return query.trim();
  }

  /**
   * Transform a {@link SearchGroup} to a String query
   * 
   * @param sg
   * @return
   * @throws UnprocessableError
   */
  private static String searchGroupToStringQuery(SearchGroup sg) {
    if (sg.getElements().size() == 1) {
      SearchElement el = sg.getElements().iterator().next();
      return transform2URL(new SearchQuery(Arrays.asList(el)));
    }
    String q = transform2URL(new SearchQuery(sg.getGroup()));
    return "".equals(q.trim()) ? "" : (sg.isNot() ? "NOT" : "") + "(" + q + ")";
  }

  /**
   * Transform a SearchPair to String query
   * 
   * @param pair
   * @return
   */
  private static String searchPairToStringQuery(SearchPair pair) {
    return (pair.isNot() ? "NOT" : "") + pair.getField() + operator2URL(pair.getOperator())
        + searchValue2URL(pair);
  }

  /**
   * Transforma a {@link SearchTechnicalMetadata} to String query
   * 
   * @param stm
   * @return
   */
  private static String searchTechnicalMetadataToStringQuery(SearchTechnicalMetadata stm) {
    return (stm.isNot() ? "NOT" : "") + SearchFields.technical + "[" + stm.getLabel() + "]"
        + operator2URL(stm.getOperator()) + searchValue2URL(stm);
  }

  /**
   * Transform a {@link SearchMetadata} to a string query
   *
   * @param statement
   * @param index
   * @return
   */
  private static String searchMetadataToStringQuery(SearchMetadata smd) {
    return (smd.isNot() ? "NOT" : "") + SearchFields.md.name() + "." + smd.getIndex()
        + (smd.getField() == null ? "" : "." + smd.getField().name())
        + operator2URL(smd.getOperator()) + searchValue2URL(smd);
  }

  /**
   * REturn the search value of the {@link SearchMetadata} as string for an url
   *
   * @param md
   * @return
   */
  private static String searchValue2URL(SearchPair pair) {
    return pair.getValue();
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
        return ">";
      case LESSER:
        return "<";
      case GREATER_EQUALS:
        return ">=";
      case LESSER_EQUALS:
        return "<=";
      case EQUALS:
        return "=";
      default:
        return "=";
    }
  }

  /**
   * Transform a {@link SearchQuery} into a user friendly query
   *
   * @param sq
   * @return
   */
  public static String searchQuery2PrettyQuery(SearchQuery sq, Locale locale) {
    try {
      return URLDecoder.decode(transform2URL(sq), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return transform2URL(sq);
    }
  }

  /**
   * Transform a {@link SearchPair} into a user friendly query
   *
   * @param pair
   * @return
   */
  private static String searchPair2PrettyQuery(SearchPair pair, Locale locale) {
    if (pair == null || pair.getField() == null || pair.getValue() == null
        || pair.getValue().equals("")) {
      return "";
    }
    if (pair.getField() == SearchFields.all) {
      return Imeji.RESOURCE_BUNDLE.getLabel("item", locale) + " = " + pair.getValue();
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
  private static String searchGroup2PrettyQuery(SearchGroup group, Locale locale) {
    String str = "";
    final int groupSize = group.getElements().size();
    if (isSearchGroupForComplexMetadata(group)) {
      for (final SearchElement md : group.getElements()) {
        if (md instanceof SearchMetadata) {
          str += searchMetadata2PrettyQuery((SearchMetadata) md, locale);
        } else if (md instanceof SearchLogicalRelation) {
          str += searchLogicalRelation2PrettyQuery((SearchLogicalRelation) md, locale);
        }
      }
    } else {
      str = searchElements2PrettyQuery(group.getElements(), locale);
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
    // final List<String> statementUris = new ArrayList<String>();
    // for (final SearchElement el : group.getElements()) {
    // if (el.getType().equals(SEARCH_ELEMENTS.METADATA)) {
    // final SearchMetadata md = (SearchMetadata) el;
    // if (statementUris.contains(md.getStatement().toString())) {
    // return true;
    // }
    // statementUris.add(md.getStatement().toString());
    // }
    // }
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
  private static String searchElements2PrettyQuery(List<SearchElement> els, Locale locale) {
    String q = "";
    for (final SearchElement el : els) {
      switch (el.getType()) {
        case PAIR:
          q += searchPair2PrettyQuery((SearchPair) el, locale);
          break;
        case GROUP:
          q += searchGroup2PrettyQuery((SearchGroup) el, locale);
          break;
        case LOGICAL_RELATIONS:
          q += searchLogicalRelation2PrettyQuery((SearchLogicalRelation) el, locale);
          break;
        case METADATA:
          q += searchMetadata2PrettyQuery((SearchMetadata) el, locale);
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
    final String orString = Imeji.RESOURCE_BUNDLE.getLabel("or_big", locale);
    final String andString = Imeji.RESOURCE_BUNDLE.getLabel("and_big", locale);
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
    final String s[] = namespace.split("/");
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
        return "=";
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
  private static String searchMetadata2PrettyQuery(SearchMetadata md, Locale locale) {
    return md.getIndex() + " " + negation2PrettyQuery(md.isNot())
        + searchOperator2PrettyQuery(md.getOperator()) + " " + md.getValue();
  }
}
