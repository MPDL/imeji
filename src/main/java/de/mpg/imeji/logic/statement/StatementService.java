package de.mpg.imeji.logic.statement;

import static java.util.stream.Collectors.toList;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchFields;
import de.mpg.imeji.logic.search.model.SearchLogicalRelation.LOGICAL_RELATIONS;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.service.SearchServiceAbstract;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.logic.vo.factory.StatementFactory;

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
   * @param index
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Statement> createBatch(List<Statement> l, User user) throws ImejiException {
    return controller.createBatch(l, user);
  }

  /**
   * Create only the statements which don't exists
   * 
   * @param l
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Statement> createBatchIfNotExists(List<Statement> l, User user)
      throws ImejiException {
    return createBatch(filterNotExistingStatement(l), user);
  }

  /**
   * Return only the Statements which don't exists
   * 
   * @param l
   * @return
   * @throws ImejiException
   */
  private List<Statement> filterNotExistingStatement(List<Statement> l) throws ImejiException {
    Map<String, Statement> map = StatementUtil.statementListToMap(retrieveAll());
    return l.stream().filter(s -> !map.containsKey(s.getIndex())).collect(Collectors.toList());
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
    Statement s = new StatementFactory().setIndex(index).build();
    return retrieve(s.getUri().toString(), user);
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
   * Filter the list with only the existing statement and retrieve it as a list of {@link Statement}
   *
   * @param ids
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<Statement> retrieveBatchOnlyExistingStatemment(List<String> uris, User user)
      throws ImejiException {
    List<Statement> statements = filterNotExistingStatement(uris.stream()
        .map(uri -> ImejiFactory.newStatement().setUri(URI.create(uri)).build()).collect(toList()));
    final List<Statement> l = controller
        .retrieveBatch(statements.stream().map(s -> s.getUri().toString()).collect(toList()), user);
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
  public Statement update(Statement before, Statement after, User user) throws ImejiException {
    if (before.getUri().equals(after.getUri())) {
      return controller.update(after, user);
    } else {
      after = controller.create(after, user);
      controller.delete(before, user);
      updateItemIndex(before, after, user);
      changeDefaultStatement(before, after);
      return after;
    }
  }

  /**
   * Change the configuration with the new default statement
   * 
   * @param before
   * @param after
   */
  private void changeDefaultStatement(Statement before, Statement after) {
    List<String> defaultStatements = Arrays.asList(Imeji.CONFIG.getStatements().split(","));
    defaultStatements = defaultStatements.stream()
        .map(s -> s = StatementUtil.indexEquals(s, before.getIndex()) ? after.getIndex() : s)
        .collect(Collectors.toList());
    String defaultStatementstring = defaultStatements.stream().collect(Collectors.joining(","));
    Imeji.CONFIG.setStatements(defaultStatementstring);
    Imeji.CONFIG.saveConfig();
  }

  /**
   * Update all item using "before" with "after"
   * 
   * @param before
   * @param after
   * @param user
   * @throws ImejiException
   */
  private void updateItemIndex(Statement before, Statement after, User user) throws ImejiException {
    ItemService itemService = new ItemService();
    SearchQuery q = new SearchFactory()
        .addElement(new SearchPair(SearchFields.index, before.getIndexUrlEncoded()),
            LOGICAL_RELATIONS.AND)
        .build();
    List<Item> items = itemService.searchAndRetrieve(q, null, user, -1, 0);
    items.stream().flatMap(item -> item.getMetadata().stream())
        .filter(md -> StatementUtil.indexEquals(before.getIndex(), md.getIndex()))
        .forEach(md -> md.setIndex(after.getIndex()));
    itemService.updateBatch(items, user);
  }

  /**
   * Delete the statement
   * 
   * @param s
   * @param user
   * @throws ImejiException
   */
  public void delete(Statement s, User user) throws ImejiException {
    if (isUsed(s)) {
      throw new NotAllowedError("Statement " + s.getIndex()
          + " is used by at least one item and therefore can't be deleted");
    }
    controller.delete(s, user);
  }

  /**
   * True if the Statement is used by any Item
   * 
   * @param s
   * @return
   * @throws ImejiException
   */
  public boolean isUsed(Statement s) throws ImejiException {
    ItemService itemService = new ItemService();
    SearchQuery q =
        new SearchFactory().addElement(new SearchPair(SearchFields.index, s.getIndexUrlEncoded()),
            LOGICAL_RELATIONS.AND).build();
    String url = SearchQueryParser.transform2UTF8URL(q);
    return itemService.search(q, null, Imeji.adminUser, 0, 0).getNumberOfRecords() > 0;
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


  public List<Statement> retrieveNotUsedStatements() throws ImejiException {
    return retrieveAll().stream().filter(s -> {
      try {
        return isUsed(s);
      } catch (ImejiException e) {
        return true;
      }
    }).collect(Collectors.toList());

  }
}
