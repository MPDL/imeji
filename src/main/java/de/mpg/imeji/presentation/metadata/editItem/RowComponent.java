package de.mpg.imeji.presentation.metadata.editItem;

import java.io.Serializable;
import java.util.Map;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.metadata.MetadataInputComponent;
import de.mpg.imeji.presentation.metadata.SelectStatementComponent;

/**
 * A row of the edit metadata form for one single Item
 *
 * @author saquet
 *
 */
public class RowComponent extends SelectStatementComponent implements Serializable {
  private static final long serialVersionUID = -5149719514250024040L;
  private MetadataInputComponent input;

  public RowComponent(Map<String, Statement> statementMap) {
    super(statementMap);
  }

  public RowComponent(Metadata metadata, Map<String, Statement> statementMap) {
    super(metadata.getStatementId(), statementMap);
    this.input = new MetadataInputComponent(metadata, statementMap.get(metadata.getStatementId()));
    System.out.println(isExists() + " " + getIndex());
  }

  @Override
  public void listener() {
    super.listener();
    this.input =
        new MetadataInputComponent(ImejiFactory.newMetadata(asStatement()).build(), asStatement());

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
