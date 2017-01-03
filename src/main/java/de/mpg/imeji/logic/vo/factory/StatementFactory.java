package de.mpg.imeji.logic.vo.factory;

import java.util.List;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;

/**
 * Factory for {@link Statement}
 *
 * @author saquet
 *
 */
public class StatementFactory {

  private final Statement statement = new Statement();

  /**
   * Build the statement
   *
   * @return
   */
  public Statement build() {
    return statement;
  }

  public StatementFactory setIndex(String index) {
    statement.setIndex(index);
    return this;
  }

  public StatementFactory setType(StatementType type) {
    statement.setType(type);
    return this;
  }

  public StatementFactory setNames(List<String> names) {
    statement.setNames(names);
    return this;
  }

  public StatementFactory addName(String name) {
    statement.getNames().add(name);
    return this;
  }
}
