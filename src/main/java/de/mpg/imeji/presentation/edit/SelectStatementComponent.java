package de.mpg.imeji.presentation.edit;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.StatementType;
import de.mpg.imeji.presentation.statement.StatementForm;

/**
 * HTML Component for Statement
 *
 * @author saquet
 *
 */
public class SelectStatementComponent implements Serializable {
  private static final long serialVersionUID = 2521052242334769127L;
  private String index;
  private Statement statement;
  private final Map<String, Statement> statementMap;
  private StatementForm statementForm = new StatementForm();

  public SelectStatementComponent(Map<String, Statement> statementMap) {
    this.statementMap = statementMap;
    this.index = null;
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
    statement = statementMap.get(index);
    statementForm.setName(index);
  }

  public void reset() {
    this.index = null;
    this.statement = null;
  }

  /**
   * Listener when the value of the component has been changed
   */
  public void listener() {
    init(index);
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
   * @return the exists
   */
  public boolean isExists() {
    return statement != null;
  }

  public List<StatementType> getTypes() {
    return Arrays.asList(StatementType.values());
  }

  /**
   * @return the statementForm
   */
  public StatementForm getStatementForm() {
    return statementForm;
  }

  /**
   * @param statementForm the statementForm to set
   */
  public void setStatementForm(StatementForm statementForm) {
    this.statementForm = statementForm;
  }

  /**
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * @param statement the statement to set
   */
  public void setStatement(Statement statement) {
    this.statement = statement;
  }


}
