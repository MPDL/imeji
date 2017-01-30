package de.mpg.imeji.presentation.edit.editSelected;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.presentation.edit.MetadataInputComponent;

/**
 * A cell of the edit selected items page
 *
 * @author saquet
 *
 */
public class CellComponent implements Serializable {
  private static final long serialVersionUID = 4617072974872823679L;
  private List<MetadataInputComponent> inputs = new ArrayList<>();
  private final Statement statement;

  public CellComponent(Statement statement, List<Metadata> metadata) {
    this.statement = statement;
    for (final Metadata m : metadata) {
      inputs.add(new MetadataInputComponent(m, statement));
    }
  }

  public List<Metadata> toMetadataList() {
    final List<Metadata> l = new ArrayList<>();
    for (final MetadataInputComponent input : inputs) {
      l.add(input.getMetadata());
    }
    return l;
  }

  /**
   * Get the index for this cell
   *
   * @return
   */
  public String getIndex() {
    return statement.getIndex();
  }

  /**
   * Add an empty value
   */
  public void addValue() {
    inputs.add(new MetadataInputComponent(ImejiFactory.newMetadata(statement).build(), statement));
  }

  /**
   * Add a metadata
   *
   * @param metadata
   */
  public void addValue(Metadata metadata) {
    inputs.add(new MetadataInputComponent(metadata, statement));
  }

  /**
   * @return the inputs
   */
  public List<MetadataInputComponent> getInputs() {
    return inputs;
  }

  /**
   * @param inputs the inputs to set
   */
  public void setInputs(List<MetadataInputComponent> inputs) {
    this.inputs = inputs;
  }

  /**
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }



}
