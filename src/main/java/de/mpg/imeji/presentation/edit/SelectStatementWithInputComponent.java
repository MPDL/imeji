package de.mpg.imeji.presentation.edit;

import java.io.Serializable;
import java.util.Map;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;

/**
 * A row of the edit metadata form for one single Item
 *
 * @author saquet
 *
 */
public class SelectStatementWithInputComponent extends SelectStatementComponent
    implements Serializable {
  private static final long serialVersionUID = -5149719514250024040L;
  private MetadataInputComponent input;

  public SelectStatementWithInputComponent(Map<String, Statement> statementMap) {
    super(statementMap);
  }

  public SelectStatementWithInputComponent(String index, Map<String, Statement> statementMap) {
    super(index, statementMap);
  }

  public SelectStatementWithInputComponent(Metadata metadata, Map<String, Statement> statementMap) {
    super(metadata.getIndex(), statementMap);
    this.input = new MetadataInputComponent(metadata, statementMap.get(metadata.getIndex()));
  }

  /**
   * Add a empty input
   */
  public void addInput() {
    this.input =
        new MetadataInputComponent(ImejiFactory.newMetadata(asStatement()).build(), asStatement());
  }

  @Override
  public void listener() {
    super.listener();
    addInput();
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
