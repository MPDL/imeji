package de.mpg.imeji.presentation.edit.selected;

import java.io.Serializable;

import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.edit.MetadataInputComponent;

/**
 * Header of the edit selected table
 * 
 * @author saquet
 *
 */
public class HeaderComponent implements Serializable {
  private static final long serialVersionUID = -2261479826163433985L;
  private MetadataInputComponent input;
  private final Statement statement;
  private boolean edit = false;
  private boolean invalidName = false;
  private String inputName;

  public HeaderComponent(Statement statement) {
    this.statement = statement.clone();
    this.setInputName(new String(statement.getIndex()));
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
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }


  /**
   * @return the invalidName
   */
  public boolean isInvalidName() {
    return invalidName;
  }

  /**
   * @param invalidName the invalidName to set
   */
  public void setInvalidName(boolean invalidName) {
    this.invalidName = invalidName;
  }

  /**
   * @return the edit
   */
  public boolean isEdit() {
    return edit;
  }

  /**
   * @param edit the edit to set
   */
  public void setEdit(boolean edit) {
    this.edit = edit;
  }

  /**
   * @return the inputName
   */
  public String getInputName() {
    return inputName;
  }

  /**
   * @param inputName the inputName to set
   */
  public void setInputName(String inputName) {
    this.inputName = inputName;
  }
}
