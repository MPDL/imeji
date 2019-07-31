package de.mpg.imeji.logic.search;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.SearchMetadataFields;
import de.mpg.imeji.logic.model.TechnicalMetadata;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.model.SearchCollectionMetadata;
import de.mpg.imeji.logic.search.model.SearchElement;
import de.mpg.imeji.logic.search.model.SearchElement.SEARCH_ELEMENTS;
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
  private static String METADATA_REGEX = "^" + SearchFields.md.name() + "\\.([a-zA-Z0-9-:_\\.]+)([=<>]{1,2})(.+)$";
  private static Pattern METADATA_PATTERN = Pattern.compile(METADATA_REGEX);
  private static String COLLECTION_METADATA_REGEX = "^(collection\\.md\\.[a-zA-Z0-9-:_\\.]+)([=<>]{1,2})(.+)$";
  private static Pattern COLLECTION_METADATA_PATTERN = Pattern.compile(COLLECTION_METADATA_REGEX);
  private static final String TECHNICAL_REGEX = SearchFields.technical.name() + "\\[(.+)\\]([=<>@]{1,2})(.+)";
  private static Pattern TECHNICAL_PATTERN = Pattern.compile(TECHNICAL_REGEX);
  private static char[] SPECIAL_CHARACTERS = {'(', ')', '=', '>', '<'};
  private static char ESCAPE_CHARACTER = '\\';

  private static final Logger LOGGER = LogManager.getLogger(SearchQueryParser.class);

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
    return parseStringQuery(query, true);
  }

  public static SearchQuery parseStringQuery(String query, boolean addFulltext) throws UnprocessableError {
    if (query == null) {
      query = "";
    }
    String decodedQuery;
    try {
      decodedQuery = URLDecoder.decode(query, "UTF-8");
      return parsedecoded(decodedQuery, addFulltext);
    } catch (final IOException e) {
      throw new UnprocessableError("Query could not be parsed: " + query);
    }
  }

  public static void main(String[] args) throws UnprocessableError {
    String q =
        "rien du tout OR (title=this is an example AND ((description=\"Super Description\" OR created=2000)) OR (md.description=\"Other Description\" OR (md.created.number>=2000 AND md.created.number<50)))";
    q = "(filetype=Image)+AND+license=no_license";
    SearchQuery sq;
    sq = parsedecoded(q, true);
    System.out.println(q);
    System.out.println(transform2URL(sq));
    q = "md.date.date= to 2000 AND md.number.number= to 50";
    sq = parsedecoded(q, true);
    System.out.println(q);
    System.out.println(transform2URL(sq));
  }

  public static SearchQuery parsedecoded(final String query, boolean addFulltext) throws UnprocessableError {
    return new SearchQuery(parseGroup(query, addFulltext).getElements());
  }

  private static SearchGroup parseGroup(String group, boolean addFulltext) throws UnprocessableError {
    final StringReader reader = new StringReader(group);
    final SearchFactory factory = new SearchFactory();
    int c = 0;
    String part = "";
    int brackets = 0;
    int index = 0;
    boolean inBracket = false;
    boolean not = false;
    LOGICAL_RELATIONS relation = LOGICAL_RELATIONS.AND;
    try {
      while ((c = reader.read()) != -1) {
        part += (char) c;
        if (c == '(' && !isEscaped(index, group)) {
          brackets++;
          inBracket = true;
        } else if (c == ')' && !isEscaped(index, group)) {
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
          SearchGroup g = parseGroup(part, addFulltext);
          g.setNot(not);
          factory.addElement(g, relation);
          part = "";
          inBracket = false;
          not = false;
        } else if (!inBracket && endsWithStopWord(part)) {
          part = part.trim();
          factory.addElement(parsePair(removeRelation(part), not, addFulltext), relation);
          relation = readRelation(part);
          part = "";
          not = false;
        }
        index++;
      }
      if (!StringHelper.isNullOrEmptyTrim(part)) {
        part = part.trim();
        factory.addElement(parsePair(removeRelation(part), not, addFulltext), relation);
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
  private static SearchElement parsePair(String s, boolean not, boolean addFulltext) throws UnprocessableError {
    if (new StringParser(METADATA_PATTERN).find(s)) {
      LOGGER.info("Detected as Metadata " + s);
      return parseMetadata(s, not);
    } else if (new StringParser(COLLECTION_METADATA_PATTERN).find(s)) {
      return parseCollectionMetadata(s, not);
    } else if (new StringParser(TECHNICAL_PATTERN).find(s)) {
      return parseTechnical(s, not);
    } else if (new StringParser(PAIR_PATTERN).find(s)) {
      return parseSearchPair(s, not);
    } else {
      if (addFulltext) {
        return new SearchFactory().or(Arrays.asList(new SearchPair(SearchFields.all, SearchOperators.EQUALS, unescape(s), not),
            new SearchPair(SearchFields.fulltext, SearchOperators.EQUALS, unescape(s), not))).buildAsGroup();
      } else {
        return new SearchPair(SearchFields.all, SearchOperators.EQUALS, unescape(s), not);
      }
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
      String value = unescape(parser.getGroup(3));
      return new SearchPair(SearchFields.valueOfIndex(index), operator, value, not);
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
      String value = unescape(parser.getGroup(3));
      String[] indexes = index.split("\\.");
      return new SearchMetadata(indexes[0], indexes.length > 1 ? SearchMetadataFields.valueOfIndex(indexes[1]) : null, operator, value,
          not);
    }
    return new SearchPair();
  }

  /**
   * Parse to a {@link SearchCollectionMetadata} following strings:
   * collection.metadata.statement=value
   * 
   * @param s
   * @return
   */
  private static SearchPair parseCollectionMetadata(String s, boolean not) {
    StringParser parser = new StringParser(COLLECTION_METADATA_PATTERN);
    if (parser.find(s)) {
      String index = parser.getGroup(1);
      SearchOperators operator = stringOperator2SearchOperator(parser.getGroup(2));
      String value = unescape(parser.getGroup(3));
      return new SearchCollectionMetadata(index, operator, value, not);
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
      String value = unescape(parser.getGroup(3));
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
  private static String removeRelation(String s) {
    return s.replace(" AND", "").replace(" OR", "").trim();
  }

  /**
   * True if the String is ended with a Search Query Stop word.
   * 
   * @param s
   * @return
   */
  private static boolean endsWithStopWord(String s) {
    return (!s.trim().equals("AND") && s.endsWith(" AND ")) || (!s.trim().equals("OR") && s.endsWith(" OR "))
        || (s.endsWith(")") && !isEscaped(s.lastIndexOf("("), s));
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
      if (SEARCH_ELEMENTS.PAIR.equals(element.getType()) && ((SearchPair) element).getField() == SearchFields.all) {
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
        case COLLECTION_METADATA:
          query += searchCollectionMetadataToStringQuery((SearchCollectionMetadata) se);
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
    return (pair.isNot() ? "NOT " : "") + pair.getField().getIndex() + operator2URL(pair.getOperator()) + searchValue2URL(pair);
  }

  /**
   * Transforma a {@link SearchTechnicalMetadata} to String query
   * 
   * @param stm
   * @return
   */
  private static String searchTechnicalMetadataToStringQuery(SearchTechnicalMetadata stm) {
    return (stm.isNot() ? "NOT " : "") + SearchFields.technical + "[" + stm.getLabel() + "]" + operator2URL(stm.getOperator())
        + searchValue2URL(stm);
  }

  /**
   * Transform a {@link SearchMetadata} to a string query
   *
   * @param statement
   * @param index
   * @return
   */
  private static String searchMetadataToStringQuery(SearchMetadata smd) {
    return (smd.isNot() ? "NOT " : "") + getMetadataIndex(smd) + operator2URL(smd.getOperator()) + searchValue2URL(smd);
  }

  /**
   * Transform a {@link SearchCollectionMetadata} to a string query
   *
   * @param statement
   * @param index
   * @return
   */
  private static String searchCollectionMetadataToStringQuery(SearchCollectionMetadata smd) {
    return (smd.isNot() ? "NOT " : "") + SearchCollectionMetadata.labelToIndex(smd.getLabel()) + operator2URL(smd.getOperator())
        + searchValue2URL(smd);
  }

  /**
   * Return the SearchMetadata index as used in the search syntatx
   * 
   * @param smd
   * @return
   */
  public static String getMetadataIndex(SearchMetadata smd) {
    return SearchFields.md.name() + "." + smd.getIndex() + (smd.getMetadataField() == null ? "" : "." + smd.getMetadataField().name());
  }

  /**
   * REturn the search value of the {@link SearchMetadata} as string for an url
   *
   * @param md
   * @return
   */
  private static String searchValue2URL(SearchPair pair) {
    return escape(pair.getValue());
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
      return unescape(URLDecoder.decode(transform2URL(sq), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      return transform2URL(sq);
    }
  }

  /**
   * True if the character at the passed index is a special character and is escaped
   * 
   * @param s
   */
  private static boolean isEscaped(int index, String s) {
    return isSpecialCharacter(s.charAt(index)) && index > 0 && s.charAt(index - 1) == ESCAPE_CHARACTER;
  }

  /**
   * True if the character is defined in SPECIAL_CHARACTERS
   * 
   * @param c
   * @return
   */
  private static boolean isSpecialCharacter(final char c) {
    return Arrays.binarySearch(SPECIAL_CHARACTERS, c) >= 0;
  }

  /**
   * Escape all SPECIAL_CHARACTERS from String
   * 
   * @param s
   * @return
   */
  private static String escape(String s) {
    StringWriter writer = new StringWriter(s.length());
    s.chars().forEachOrdered(c -> writer.append(escape((char) c)));
    return writer.toString();
  }

  /**
   * Unescape all SPECIAL_CHARACTERS from String
   * 
   * @param s
   * @return
   */
  private static String unescape(String s) {
    return s.replace(Character.toString(ESCAPE_CHARACTER), "");
  }

  /**
   * Escape character if is special
   * 
   * @param c
   * @return
   */
  private static CharSequence escape(char c) {
    if (isSpecialCharacter(c)) {
      return Character.toString(ESCAPE_CHARACTER) + Character.toString(c);
    }
    return new String(new char[] {c});
  }

}
