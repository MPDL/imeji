package de.mpg.imeji.presentation.edit.selected;

import java.io.Serializable;
import java.util.Map;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.edit.MetadataInputComponent;
import de.mpg.imeji.presentation.edit.SelectStatementComponent;

/**
 * Header of the edit selected table
 * 
 * @author saquet
 *
 */
public class HeaderComponent implements Serializable {
  private static final long serialVersionUID = -2261479826163433985L;
  private MetadataInputComponent input;
  private SelectStatementComponent selectStatementComponent;
  private Statement statement;

  public HeaderComponent(Statement statement, Map<String, Statement> statementMap) {
    this.statement = statement;
    selectStatementComponent = new SelectStatementComponent(statement.getIndex(), statementMap);
  }

  public void initInput() {
    input = new MetadataInputComponent(ImejiFactory.newMetadata(statement).build(), statement);
  }

  /**
   * @return the inputComponent
   */
  public MetadataInputComponent getInput() {
    return input;
  }

  /**
   * @param inputComponent the inputComponent to set
   */
  public void setInput(MetadataInputComponent inputComponent) {
    this.input = inputComponent;
  }

  /**
   * @return the selectStatementComponent
   */
  public SelectStatementComponent getSelect() {
    return selectStatementComponent;
  }

  /**
   * @param selectStatementComponent the selectStatementComponent to set
   */
  public void setSelect(SelectStatementComponent selectStatementComponent) {
    this.selectStatementComponent = selectStatementComponent;
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
