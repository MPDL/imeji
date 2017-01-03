package de.mpg.imeji.logic.statement;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.service.ImejiControllerAbstract;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * Controller for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementController extends ImejiControllerAbstract<Statement> {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.statementModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.statementModel);

  @Override
  public List<Statement> createBatch(List<Statement> l, User user) throws ImejiException {
    WRITER.create(toObjectList(l), user);
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
    WRITER.update(toObjectList(l), user, true);
    return l;
  }

  @Override
  public void deleteBatch(List<Statement> l, User user) throws ImejiException {
    WRITER.delete(toObjectList(l), user);
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
