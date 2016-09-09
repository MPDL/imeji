/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.search.jenasearch;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.j2j.transaction.SPARQLUpdateTransaction;
import de.mpg.imeji.j2j.transaction.SearchTransaction;
import de.mpg.imeji.j2j.transaction.ThreadedTransaction;
import de.mpg.imeji.logic.Imeji;

/**
 * Manage search (sparql) transaction
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ImejiSPARQL {

  private ImejiSPARQL() {
    // private constructor
  }

  /**
   * Execute a sparql query and return {@link List} of uri
   *
   * @param query
   * @param c
   * @return
   */
  public static List<String> exec(String query, String modelName) {
    List<String> results = new ArrayList<String>();
    SearchTransaction transaction = new SearchTransaction(modelName, query, results, false);
    transaction.start(Imeji.dataset);
    try {
      transaction.throwException();
    } catch (ImejiException e) {
      Logger.getLogger(ImejiSPARQL.class).error("There has been some SPARQL issue", e);
    }
    return results;
  }

  /**
   * Example: SELECT ?s count(DISTINCT ?s) WHERE { ?s a <http://imeji.org/terms/item>}
   *
   * @param query
   * @param modelURI
   * @return
   */
  public static int execCount(String query, String modelName) {
    query = query.replace("SELECT DISTINCT ?s WHERE ", "SELECT count(DISTINCT ?s) WHERE ");
    List<String> results = new ArrayList<String>(1);
    SearchTransaction transaction = new SearchTransaction(modelName, query, results, true);
    transaction.start(Imeji.dataset);
    try {
      transaction.throwException();
    } catch (ImejiException e) {
      Logger.getLogger(ImejiSPARQL.class).error("There has been execCount issue", e);
    }
    if (results.size() > 0) {
      return Integer.parseInt(results.get(0));
    }
    return 0;
  }

  /**
   * execute a sparql update
   *
   * @param query
   */
  public static void execUpdate(String query) {
    try {
      ThreadedTransaction
          .run(new ThreadedTransaction(new SPARQLUpdateTransaction(null, query), Imeji.tdbPath));
    } catch (ImejiException e) {
      throw new RuntimeException(e);
    }
  }
}
