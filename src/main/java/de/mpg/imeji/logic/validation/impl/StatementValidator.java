package de.mpg.imeji.logic.validation.impl;

import java.util.HashSet;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.db.repositories.StatementDbRepository;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchResult;

/**
 * Validator for {@link Statement}
 * 
 * @author saquet
 *
 */
public class StatementValidator extends ObjectValidator implements Validator<Statement> {
  private UnprocessableError exception = new UnprocessableError(new HashSet<String>());

  @Override
  public void validate(Statement statement, Method method) throws UnprocessableError {
    exception = new UnprocessableError();

    if (indexAlreadyUsed(statement)) {
      exception = new UnprocessableError("Statement name already used", exception);
    }

    if (exception.hasMessages()) {
      throw exception;
    }
  }

  /**
   * True if the index is already used by another statement
   * 
   * @param statement
   * @return
   */
  private boolean indexAlreadyUsed(Statement statement) {

    try {
      List<Statement> result = new StatementDbRepository().readByIndex(statement.getIndex());
      if (result.size() > 0) {
        return !result.get(0).getType().equals(statement.getType());
      }
      return false;
    } catch (ImejiException e) {
      throw new RuntimeException(e);
    }

    /*
    final Search search = SearchFactory.create(SearchObjectTypes.STATEMENT, SEARCH_IMPLEMENTATIONS.JENA);
    final SearchResult result = search.searchString(JenaCustomQueries.selectStatementTypeByIndex(statement.getIndex()), null, null,
        Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS);
    */



  }

}
