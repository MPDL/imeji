package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.rest.to.predefinedMetadataTO.MetadataTO;
import de.mpg.imeji.util.LocalizedString;

@XmlRootElement
@XmlType(propOrder = {"position", "Labels", "value", "statementUri", "typeUri",})
@JsonInclude(Include.NON_NULL)
@JsonDeserialize(using = MetadataSetTODeserializer.class)
public class MetadataSetTO implements Serializable {

  private static final long serialVersionUID = 5826924314949469841L;

  public MetadataSetTO() {

  }

  /**
   * Construct a {@link MetadataSetTO} from a {@link StatementTO}
   *
   * @param statement
   */
  public MetadataSetTO(StatementTO statement) {
    // TODO refactor statementTO with LocalizedStringTO
    for (LocalizedString label : statement.getLabels()) {
      labels.add(new LabelTO(label.getLang(), label.getValue()));
    }
    this.statementUri = ObjectHelper.getURI(Statement.class, statement.getId());
    this.typeUri = statement.getType();
    this.parentStatementUri = statement.getParentStatementId();
  }

  @JsonIgnore
  private int position;

  private List<LabelTO> labels = new ArrayList<LabelTO>();

  private MetadataTO value;

  private URI statementUri;

  private URI typeUri;

  private String parentStatementUri;

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public List<LabelTO> getLabels() {
    return labels;
  }

  public void setLabels(List<LabelTO> labels) {
    this.labels = labels;
  }

  public MetadataTO getValue() {
    return value;
  }

  public void setValue(MetadataTO value) {
    this.value = value;

  }

  public URI getStatementUri() {
    return statementUri;
  }

  public void setStatementUri(URI statementUri) {
    this.statementUri = statementUri;
  }

  public URI getTypeUri() {
    return typeUri;
  }

  public void setTypeUri(URI typeUri) {
    this.typeUri = typeUri;
  }

  public String getParentStatementUri() {
    return parentStatementUri;
  }

  public void setParentStatementUri(String statementUri) {
    this.parentStatementUri = statementUri;
  }

}
