package de.mpg.imeji.logic.statement;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.service.SearchServiceAbstract;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;

/**
 * Service for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementService extends SearchServiceAbstract<Statement> {
  private final StatementController controller = new StatementController();

  public StatementService() {
    super(SearchObjectTypes.STATEMENT);
  }

  /**
   * Create a {@link Statement}
   *
   * @param statement
   * @param user
   * @return
   * @throws ImejiException
   */
  public Statement create(Statement statement, User user) throws ImejiException {
    return controller.create(statement, user);
  }

  /**
   * Create a list of {@link Statement}
   *
   * @param statement
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Statement> createBatch(List<Statement> l, User user) throws ImejiException {
    return controller.createBatch(l, user);
  }

  /**
   * Retrieve a {@link Statement}
   *
   * @param id
   * @param user
   * @return
   * @throws ImejiException
   */
  public Statement retrieve(String uri, User user) throws ImejiException {
    return controller.retrieve(uri, user);
  }

  /**
   * Retrieve a Statement according to its index
   * 
   * @param index
   * @param user
   * @return
   * @throws ImejiException
   */
  public Statement retrieveByIndex(String index, User user) throws ImejiException {
    return retrieve(
        ObjectHelper.getURI(Statement.class, StatementUtil.encodeIndex(index)).toString(), user);
  }

  /**
   * Retrieve a list of {@link Statement}
   *
   * @param ids
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Statement> retrieveBatch(List<String> uris, User user) throws ImejiException {
    final List<Statement> l = controller.retrieveBatch(uris, user);
    l.sort((s1, s2) -> s1.getIndex().compareToIgnoreCase(s2.getIndex()));
    return l;
  }

  /**
   * Update a {@link Statement}
   *
   * @param statement
   * @param user
   * @return
   * @throws ImejiException
   */
  public Statement update(Statement statement, User user) throws ImejiException {
    return controller.update(statement, user);
  }

  /**
   * Delete the statement
   * 
   * @param s
   * @param user
   * @throws ImejiException
   */
  public void delete(Statement s, User user) throws ImejiException {
    controller.delete(s, user);
  }

  /**
   * Return the index for the passed statement id
   * 
   * @param id
   * @return
   */
  public String getIndex(String id) {
    return "";
  }

  /**
   * Merge statement1 into statement2
   *
   * @param statement1
   * @param statement2
   * @return
   */
  public Statement merge(Statement statement1, Statement statement2) throws ImejiException {
    return statement2;
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    final SearchResult result = new SearchResult(
        ImejiSPARQL.exec(JenaCustomQueries.selectStatementAll(), Imeji.statementModel));
    return result;
  }

  @Override
  public List<Statement> retrieve(List<String> ids, User user) throws ImejiException {
    return retrieveBatch(ids, Imeji.adminUser);
  }

  @Override
  public List<Statement> retrieveAll() throws ImejiException {
    final List<String> uris =
        ImejiSPARQL.exec(JenaCustomQueries.selectStatementAll(), Imeji.statementModel);
    return retrieveBatch(uris, Imeji.adminUser);
  }
}
