package de.mpg.imeji.rest.to;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.JsonNode;

import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.rest.transfer.MetadataTransferHelper;

@XmlRootElement
@JsonInclude(Include.NON_EMPTY)

/**
 * Bean for all Metadata types. This bean should have all variable that have been defined in all
 * metadata types.
 *
 * @author saquet
 */
public class SuperMetadataBeanTO implements Comparable<SuperMetadataBeanTO>, Serializable {
  private static final long serialVersionUID = 5166665303590747237L;
  /**
   * The {@link Metadata} defined within thie {@link SuperMetadataBeanTO}
   */
  @JsonIgnore
  private Metadata metadata;
  /**
   * The position of the {@link Metadata} in the {@link MetadataSet}
   */
  @JsonIgnore
  private int pos = 0;
  /**
   * The parent {@link SuperMetadataBeanTO} (i.e {@link Metadata}), according to what is defined in
   * the {@link MetadataProfile}
   */
  @JsonBackReference
  private SuperMetadataBeanTO parent = null;
  /**
   * Define the position if the metadata in the {@link SuperMetadataTreeTO}
   */

  @JsonIgnore
  private String treeIndex = "";
  /**
   * The {@link Statement} of this {@link Metadata}
   */
  @JsonIgnore
  private Statement statement;
  /**
   * All the childs of the metadata
   */

  @JsonIgnore
  private String label;
  @JsonIgnore
  private List<JsonNode> values = new ArrayList<JsonNode>();


  private List<JsonNode> jsonField;
  @JsonManagedReference
  private List<SuperMetadataBeanTO> childs = new ArrayList<SuperMetadataBeanTO>();

  private JsonNode jsonFieldSingle;

  /**
   * A field where it is possible to define many other fields
   */
  @JsonIgnore
  private String customField;

  /**
   * Bean for all Metadata types. This bean should have all variable that have been defined in all
   * metadata types.
   *
   * @param metadata
   */
  public SuperMetadataBeanTO(Metadata metadata, Statement statement) {
    ObjectHelper.copyAllFields(metadata, this);
    this.metadata = metadata;
    this.statement = statement;
    JsonNode nodeVal = MetadataTransferHelper.serializeMetadata(metadata, statement);
    values.add(nodeVal);
    this.label = statement.getLabel();
  }

  public SuperMetadataBeanTO(Statement statement) {
    this.statement = statement;
  }


  /**
   * Return the {@link Metadata} which has been used to initialize this {@link SuperMetadataBeanTO}
   * Not to use to save the {@link SuperMetadataBeanTO} as a {@link Metadata} in the database. In
   * this case use the asMetadata() method
   *
   * @return
   */
  @JsonIgnore
  public Metadata getMetadata() {
    return metadata;
  }

  /**
   * Retun the id (last part of the {@link URI}) of the {@link Statement}. Used for GUI
   * representation
   *
   * @return
   */
  @JsonIgnore
  public String getStatementId() {
    return ObjectHelper.getId(getStatement().getId());
  }


  /**
   * getter
   *
   * @return
   */
  public String getLabel() {
    return label;
  }

  /**
   * setter
   *
   * @param label
   */
  public void setLabel(String label) {
    this.label = label;
  }


  /**
   * getter
   *
   * @return
   */
  public int getPos() {
    return pos;
  }

  /**
   * setter
   *
   * @param pos
   */
  public void setPos(int pos) {
    this.pos = pos;
  }

  /**
   * setter
   *
   * @param parent the parent to set
   */
  public void setParent(SuperMetadataBeanTO parent) {
    this.parent = parent;
  }

  /**
   * getter
   *
   * @return the parent
   */
  public SuperMetadataBeanTO getParent() {
    return parent;
  }

  /**
   * getter
   *
   * @return the hierarchyLevel
   */

  @JsonIgnore
  public int getHierarchyLevel() {
    return (getTreeIndex().length() - 1) / 2;
  }


  /**
   * getter
   *
   * @return the statement
   */
  public Statement getStatement() {
    return statement;
  }

  /**
   * setter
   *
   * @param statement the statement to set
   */
  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  @JsonIgnore
  public String getLastParentTreeIndex() {
    return treeIndex.split(",", 0)[0];
  }

  /**
   * Return the higher parent
   *
   * @return
   */
  public SuperMetadataBeanTO lastParent() {
    SuperMetadataBeanTO smb = this;
    while (smb.getParent() != null) {
      smb = smb.getParent();
    }
    return smb;
  }

  /**
   * @return the childs
   */

  public List<SuperMetadataBeanTO> getChilds() {
    return childs;
  }

  /**
   * @param childs the childs to set
   */
  public void setChilds(List<SuperMetadataBeanTO> childs) {
    this.childs = childs;
  }


  /**
   * @return the treeIndex
   */
  public String getTreeIndex() {
    return treeIndex;
  }

  /**
   * @param treeIndex the treeIndex to set
   */
  public void setTreeIndex(String treeIndex) {
    this.treeIndex = treeIndex;
  }

  /**
   * @return the jsonField
   */
  public List<JsonNode> getJsonField() {
    return jsonField;
  }

  public JsonNode getJsonFieldSingle() {
    return jsonFieldSingle;
  }

  /**
   * @param jsonField the jsonField to set
   */
  public void setJsonField(List<JsonNode> jsonField) {
    this.jsonField = jsonField;
  }

  /**
   * @param jsonField the jsonField to set
   */
  public void setJsonFieldList(List<JsonNode> jsonField) {

    if (!isMultiple()) {
      this.jsonFieldSingle = jsonField.get(0);
    } else {
      this.jsonField = jsonField;
    }
  }

  public boolean isMultiple() {
    return !statement.getMaxOccurs().equals("1");
  }


  @Override
  public int compareTo(SuperMetadataBeanTO o) {
    if (statement.getPos() > o.getStatement().getPos()) {
      return 1;
    } else if (statement.getPos() < o.getStatement().getPos()) {
      return -1;
    }
    if (getPos() > o.getPos()) {
      return 1;
    } else if (getPos() < o.getPos()) {
      return -1;
    }
    return 0;
  }



}
