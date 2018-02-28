package de.mpg.imeji.presentation.item.edit.single;

import java.io.Serializable;
import java.util.Map;

import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.presentation.item.edit.MetadataInputComponent;
import de.mpg.imeji.presentation.item.edit.SelectStatementComponent;

/**
 * Entry of the {@link EditItemComponent}
 * 
 * @author saquet
 *
 */
public class EditItemEntry implements Serializable {
  private static final long serialVersionUID = 8538919630931186984L;
  private SelectStatementComponent select;
  private MetadataInputComponent input;

  public EditItemEntry(Map<String, Statement> statementMap) {
    select = new SelectStatementComponent(statementMap);
  }

  public EditItemEntry(Metadata metadata, Map<String, Statement> statementMap) {
    input = new MetadataInputComponent(metadata, statementMap.get(metadata.getIndex()));
    select = new SelectStatementComponent(statementMap);
  }

  public void initInput() {
    Statement statement = select.asStatement();
    input = new MetadataInputComponent(ImejiFactory.newMetadata(statement).build(), statement);
  }

  public void resetInput() {
    input = null;
    select.setIndex(null);
    select.setStatement(null);
  }

  /**
   * @return the select
   */
  public SelectStatementComponent getSelect() {
    return select;
  }

  /**
   * @param select the select to set
   */
  public void setSelect(SelectStatementComponent select) {
    this.select = select;
  }

  /**
   * @return the input
   */
  public MetadataInputComponent getInput() {
    return input;
  }

  /**
   * @param input the input to set
   */
  public void setInput(MetadataInputComponent input) {
    this.input = input;
  }
}
