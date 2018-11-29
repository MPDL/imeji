package de.mpg.imeji.test.logic.search;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;

/**
 * Tests for the methods in {@link SearchQueryParser}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class URLQueryTransformerTest {
  /**
   * TODO Non working characters: ()+=
   */
  private static String specialsChar =
      "japanese:テスト  chinese:實驗 yiddish:פּראָבע arab:اختبار bengali: পরীক্ষা other:öäü@ß$&@*~!?{}[]-#'.,áò";
  private static String advancedQuery =
      "col=\"http://imeji.org/collection/86\" AND md.title.text=TEST AND (md.created.date=2012 OR md.location.placename=Munich)";
  private static String simpleQuery = "TEST";

  /**
   * Test the methods for an advanced search query
   * 
   * @throws IOException
   * @throws UnprocessableError
   */
  @Test
  public void testAdvancedSearch() throws IOException, UnprocessableError {
    test(advancedQuery, false);
  }

  /**
   * Test the methods for an simple search query
   * 
   * @throws IOException
   * @throws UnprocessableError
   */
  @Test
  public void testSimpleSeach() throws IOException, UnprocessableError {
    test(simpleQuery, true);
  }

  /**
   * Test the methods for an advanced search query with special characters
   * 
   * @throws IOException
   * @throws UnprocessableError
   */
  @Test
  public void testAdvancedSearchWithSpecialCharacter() throws IOException, UnprocessableError {
    test(advancedQuery.replace("TEST", specialsChar), false);
  }

  /**
   * Test the methods for an simple search query with special characters
   * 
   * @throws IOException
   * @throws UnprocessableError
   */
  @Test
  public void testSimpleSearchWithSpecialCharacter() throws IOException, UnprocessableError {
    test(simpleQuery.replace("TEST", specialsChar), true);
  }

  /**
   * Make the test for one String (encoded and non encoded)
   * 
   * @param query
   * @throws IOException
   * @throws UnprocessableError
   * @throws UnsupportedEncodingException
   */
  private void test(String query, boolean simple) throws UnprocessableError, UnsupportedEncodingException {
    String encodedQuery = URLEncoder.encode(query, "UTF-8");
    SearchQuery sq = SearchQueryParser.parseStringQuery(query);
    String resultEncoded = SearchQueryParser.transform2UTF8URL(sq);
    String resultNotCoded = SearchQueryParser.transform2URL(sq);
    // Prepare the query for comparison
    // remove non relevant spaces
    query = query.trim();
    encodedQuery = encodedQuery.trim();
    // Set the simple query
    if (simple) {
      query = toSimpleQuery(query);
      encodedQuery = URLEncoder.encode(query, "UTF-8");
    }
    Assert.assertEquals(query, resultNotCoded);
    Assert.assertEquals(encodedQuery, resultEncoded);
  }

  /**
   * Transform a string query to a imeji simple query
   * 
   * @param q
   * @return
   */
  private String toSimpleQuery(String q) {
    return "(all=" + q + " OR fulltext=" + q + ")";
  }
}
