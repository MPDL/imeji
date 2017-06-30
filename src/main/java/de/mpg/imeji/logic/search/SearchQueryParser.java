package de.mpg.imeji.logic.search;

import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Pattern;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchGroup;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchMetadata;
import de.mpg.imeji.logic.search.model.SearchMetadataFields;
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
  private static String PAIR_REGEX = "([a-zA-Z0-9-_\\.]+)([=<>]{1,2})(.+)";
  private static Pattern PAIR_PATTERN = Pattern.compile(PAIR_REGEX);
  private static String METADATA_REGEX =
      SearchFields.md.name() + "\\.([a-zA-Z0-9-_\\.]+)([=<>]{1,2})(.+)";
  private static Pattern METADATA_PATTERN = Pattern.compile(METADATA_REGEX);
  private static final String TECHNICAL_REGEX =
      SearchFields.technical.name() + "\\[(.+)\\]([=<>@]{1,2})(.+)";
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
        "rinew du tounbt OR (title=this is an example AND (description=\"Super Description\" OR created=2000) OR (md.description=\"Other Description\" OR (md.created.number>=2000 AND md.created.number<50)))";
    SearchQuery sq;
    // sq = parsedecoded(q);
    // System.out.println(q);
    // System.out.println(transform2URL(sq));
    // q = "(md.title.text=hi OR md.description.text=Bob OR md.date.text=2017-02-08)";
    // sq = parsedecoded(q);
    // System.out.println(q);
    // System.out.println(transform2URL(sq));
    // q = "md.location.coordinates=5.0,7.0";
    // sq = parsedecoded(q);
    // System.out.println(q);
    // System.out.println(transform2URL(sq));
    // q = "NOT (filename=Tulips.jpg AND filename=Desert.jpg)";
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
    boolean not = false;
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
        if ("NOT".equals(part.trim())) {
          not = true;
          part = "";
        }
        if (inBracket && brackets == 0) {
          // remove unnecessary spaces
          part = part.trim();
          // remove brackets around the group
          part = part.substring(1, part.length() - 1);
          SearchGroup g = parseGroup(part);
          g.setNot(not);
          factory.addElement(g, relation);
          part = "";
          inBracket = false;
          not = false;
        } else if (!inBracket && endsWithStopWord(part)) {
          part = part.trim();
          factory.addElement(parsePair(removeStopWord(part), not), relation);
          relation = readRelation(part);
          part = "";
          not = false;
        }
      }
      if (!StringHelper.isNullOrEmptyTrim(part)) {
        part = part.trim();
        factory.addElement(parsePair(removeStopWord(part), not), relation);
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
   * @throws UnprocessableError
   */
  private static SearchElement parsePair(String s, boolean not) throws UnprocessableError {
    if (new StringParser(METADATA_PATTERN).find(s)) {
      return parseMetadata(s, not);
    } else if (new StringParser(TECHNICAL_PATTERN).find(s)) {
      return parseTechnical(s, not);
    } else if (new StringParser(PAIR_PATTERN).find(s)) {
      return parseSearchPair(s, not);
    } else {
      return new SearchFactory()
          .or(Arrays.asList(new SearchPair(SearchFields.all, SearchOperators.EQUALS, s, not),
              new SearchPair(SearchFields.fulltext, SearchOperators.EQUALS, s, not)))
          .buildAsGroup();
    }
  }

  /**
   * Parse to a {@link SearchPair} following string: index=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseSearchPair(String s, boolean not) {
    StringParser parser = new StringParser(PAIR_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      return new SearchPair(SearchFields.valueOf(index), operator, value, not);
    }
    return new SearchPair();
  }

  /**
   * Parse to a {@link SearchMetadata} following strings: md.statement.index=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseMetadata(String s, boolean not) {
    StringParser parser = new StringParser(METADATA_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      String[] indexes = index.split("\\.");
      return new SearchMetadata(indexes[0],
          indexes.length > 1 ? SearchMetadataFields.valueOf(indexes[1]) : null, operator, value,
          not);
    }
    return new SearchPair();
  }

  /**
   * Parse to a {@link TechnicalMetadata} following strings: technical[label]=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseTechnical(String s, boolean not) {
    StringParser parser = new StringParser(TECHNICAL_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = parser.getGroup(3);
      return new SearchTechnicalMetadata(operator, value, index, not);
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
    String q = transform2URL(new SearchQuery(sg.getGroup()));
    return "".equals(q.trim()) ? "" : (sg.isNot() ? "NOT " : "") + "(" + q + ")";
  }

  /**
   * Transform a SearchPair to String query
   * 
   * @param pair
   * @return
   */
  private static String searchPairToStringQuery(SearchPair pair) {
    return (pair.isNot() ? "NOT " : "") + pair.getField() + operator2URL(pair.getOperator())
        + searchValue2URL(pair);
  }

  /**
   * Transforma a {@link SearchTechnicalMetadata} to String query
   * 
   * @param stm
   * @return
   */
  private static String searchTechnicalMetadataToStringQuery(SearchTechnicalMetadata stm) {
    return (stm.isNot() ? "NOT " : "") + SearchFields.technical + "[" + stm.getLabel() + "]"
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
    return (smd.isNot() ? "NOT " : "") + getMetadataIndex(smd) + operator2URL(smd.getOperator())
        + searchValue2URL(smd);
  }

  /**
   * Return the SearchMetadata index as used in the search syntatx
   * 
   * @param smd
   * @return
   */
  public static String getMetadataIndex(SearchMetadata smd) {
    return SearchFields.md.name() + "." + smd.getIndex()
        + (smd.getMetadataField() == null ? "" : "." + smd.getMetadataField().name());
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
  public static String operator2URL(SearchOperators op) {
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
  public static String searchQuery2PrettyQuery(SearchQuery sq) {
    try {
      return URLDecoder.decode(transform2URL(sq), "UTF-8");
    } catch (UnsupportedEncodingException e) {
      return transform2URL(sq);
    }
  }
}
