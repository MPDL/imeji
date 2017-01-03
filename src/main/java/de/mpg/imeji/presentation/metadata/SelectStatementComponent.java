package de.mpg.imeji.presentation.metadata;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.logic.vo.factory.StatementFactory;

/**
 * HTML Component for Statement
 *
 * @author saquet
 *
 */
public class SelectStatementComponent implements Serializable {
  private static final long serialVersionUID = 2521052242334769127L;
  private String index;
  private StatementType type;
  private boolean exists = false;
  private final Map<String, Statement> statementMap;

  public SelectStatementComponent(Map<String, Statement> statementMap) {
    this.type = StatementType.TEXT;
    this.statementMap = statementMap;
  }

  public SelectStatementComponent(String index, Map<String, Statement> statementMap) {
    this(statementMap);
    init(index);
  }

  /**
   * Initialize the component
   *
   * @param index
   */
  private void init(String index) {
    this.index = index;
    final Statement s = statementMap.get(index);
    this.exists = s != null;
    if (exists) {
      this.type = s.getType();
    }
  }

  /**
   * Listener when the value of the component has been changed
   */
  public void listener() {
    init(index);
  }

  public Statement asStatement() {
    return new StatementFactory().setIndex(index).setType(type).build();
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index;
  }

  /**
   * @return the type
   */
  public StatementType getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(StatementType type) {
    this.type = type;
  }

  /**
   * @return the exists
   */
  public boolean isExists() {
    return exists;
  }

  public List<StatementType> getTypes() {
    return Arrays.asList(StatementType.values());
  }

}
