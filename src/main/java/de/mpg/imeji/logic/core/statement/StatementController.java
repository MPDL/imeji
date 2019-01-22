package de.mpg.imeji.logic.core.statement;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.generic.ImejiControllerAbstract;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Controller for {@link Statement}
 *
 * @author saquet
 *
 */
class StatementController extends ImejiControllerAbstract<Statement> {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.statementModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.statementModel);

  @Override
  public List<Statement> createBatch(List<Statement> l, User user) throws ImejiException {
    WRITER.create(toObjectList(filterDuplicate(l)), Imeji.adminUser);
    return l;
  }

  @Override
  public List<Statement> retrieveBatch(List<String> ids, User user) throws ImejiException {
    final List<Statement> statements = initializeEmtpyList(ids);
    READER.read(toObjectList(statements), user);
    return statements;
  }

  @Override
  public List<Statement> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
    return retrieveBatch(ids, user);
  }

  @Override
  public List<Statement> updateBatch(List<Statement> l, User user) throws ImejiException {
    WRITER.update(toObjectList(filterDuplicate(l)), user, true);
    return l;
  }

  @Override
  public void deleteBatch(List<Statement> l, User user) throws ImejiException {
    WRITER.delete(toObjectList(filterDuplicate(l)), user);
  }

  /**
   * Filter all duplicate statements (i.e. same index) of this list out
   * 
   * @param l
   * @return
   */
  private List<Statement> filterDuplicate(List<Statement> l) {
    return new ArrayList<>(l.stream().filter(s -> !StringHelper.isNullOrEmptyTrim(s.getIndex()))
        .collect(Collectors.toMap(Statement::getIndex, Function.identity())).values());
  }

  /**
   * Initialize a list of empty statements with their id
   *
   * @param ids
   * @return
   */
  private List<Statement> initializeEmtpyList(List<String> ids) {
    final List<Statement> statements = new ArrayList<>(ids.size());
    for (final String id : ids) {
      final Statement st = new Statement();
      st.setUri(URI.create(id));
      statements.add(st);
    }
    return statements;
  }

}
