package de.mpg.imeji.presentation.edit;

import java.io.Serializable;

import de.mpg.imeji.logic.model.Metadata;
import de.mpg.imeji.logic.model.Statement;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.model.util.MetadataUtil;

/**
 * Component for the html input of {@link Metadata}
 *
 * @author saquet
 *
 */
public class MetadataInputComponent implements Serializable {
  private static final long serialVersionUID = 4723925226621344678L;
  private Metadata metadata;
  private Statement statement;

  public MetadataInputComponent(Metadata metadata, Statement statement) {
    this.metadata = metadata.copy();
    if (metadata.getPerson() == null) {
      this.metadata.setPerson(ImejiFactory.newPerson());
    }
    this.statement = statement;
  }

  /**
   * True if the current {@link Metadata} is empty
   *
   * @return
   */
  public boolean isEmpty() {
    return MetadataUtil.isEmpty(metadata);
  }

  /**
   * @return the metadata
   */
  public Metadata getMetadata() {
    return metadata;
  }

  /**
   * @param metadata the metadata to set
   */
  public void setMetadata(Metadata metadata) {
    this.metadata = metadata;
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
