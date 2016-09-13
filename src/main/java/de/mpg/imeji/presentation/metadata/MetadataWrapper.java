package de.mpg.imeji.presentation.metadata;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.event.ValueChangeEvent;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.util.DateFormatter;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.ImejiFactory;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.MetadataFactory;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.presentation.metadata.util.MetadataHelper;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.CommonUtils;
import de.mpg.imeji.presentation.util.SearchAndExportHelper;

/**
 * Bean for all Metadata types. This bean should have all variable that have been defined in all
 * metadata types.
 *
 * @author saquet
 */
public class MetadataWrapper implements Comparable<MetadataWrapper>, Serializable {
  private static final long serialVersionUID = 5166665303590747237L;
  /**
   * The {@link Metadata} defined within thie {@link MetadataWrapper}
   */
  private Metadata metadata;
  /**
   * The position of the {@link Metadata} in the {@link MetadataSet}
   */
  private int pos = 0;
  /**
   * The parent {@link MetadataWrapper} (i.e {@link Metadata}), according to what is defined in the
   * {@link MetadataProfile}
   */
  private MetadataWrapper parent = null;
  /**
   * Define the position if the metadata in the {@link MetadataWrapperTree}
   */
  private String treeIndex = "";
  /**
   * The {@link Statement} of this {@link Metadata}
   */
  private Statement statement;
  /**
   * All the childs of the metadata
   */
  private List<MetadataWrapper> childs = new ArrayList<MetadataWrapper>();
  private boolean preview = true;
  private boolean toNull = false;
  private boolean resetCitation = false;
  // All possible fields defined for a metadata:
  private String text;
  private Person person;
  private URI coneId;
  private URI uri;
  private String label;
  private String date;
  private long time;
  private double longitude = Double.NaN;
  private double latitude = Double.NaN;
  private String name;
  private String exportFormat;
  private String citation;
  private double number = Double.NaN;
  private String license = null;
  private URI externalUri;
  /**
   * A field where it is possible to define many other fields
   */
  private String customField;

  /**
   * Bean for all Metadata types. This bean should have all variable that have been defined in all
   * metadata types.
   *
   * @param metadata
   */
  public MetadataWrapper(Metadata metadata, Statement statement) {
    ObjectHelper.copyAllFields(metadata, this);
    this.metadata = metadata;
    this.preview = statement.isPreview();
    this.statement = statement;
    this.customField = CommonUtils.toStringCustomField(metadata);
  }

  public MetadataWrapper(Statement statement) {
    this.preview = statement.isPreview();
    this.statement = statement;
  }

  /**
   * Get {@link MetadataWrapper} as {@link Metadata}
   *
   * @return
   */
  public Metadata asMetadata() {
    if (resetCitation) {
      resetCitation();
    }
    ObjectHelper.copyAllFields(this, metadata);
    MetadataHelper.setConeID(metadata);
    return metadata;
  }

  /**
   * Return the {@link Metadata} which has been used to initialize this {@link MetadataWrapper} Not
   * to use to save the {@link MetadataWrapper} as a {@link Metadata} in the database. In this case
   * use the asMetadata() method
   *
   * @return
   */
  public Metadata getMetadata() {
    return metadata;
  }

  /**
   * Change the Id of the {@link Metadata} which will force the create a new {@link Metadata}
   * resource in the database
   *
   * @return a new {@link MetadataWrapper} with the same values
   */
  public MetadataWrapper copy() {
    metadata.setId(IdentifierUtil.newURI(Metadata.class));
    MetadataWrapper copy =
        new MetadataWrapper(MetadataFactory.copyMetadata(asMetadata()), statement);
    copy.setParent(parent);
    copy.setTreeIndex(treeIndex);
    return copy;
  }

  /**
   * Copy this {@link MetadataWrapper} without the values
   *
   * @return a new {@link MetadataWrapper} with the same values
   */
  public MetadataWrapper copyEmpty() {
    metadata.setId(IdentifierUtil.newURI(Metadata.class));
    MetadataWrapper copy =
        new MetadataWrapper(MetadataFactory.createMetadata(statement), statement);
    copy.setParent(parent);
    copy.setTreeIndex(treeIndex);
    return copy;
  }

  /**
   * Clear all values
   */
  public void clear() {
    ObjectHelper.copyAllFields(copyEmpty(), this);
  }

  /**
   * Listener that check when a select menu has been set to null
   *
   * @param vce
   */
  public void predefinedValueListener(ValueChangeEvent vce) {
    Object newValue = vce.getNewValue();
    if (newValue == null) {
      clear();
      toNull = true;
    } else if (newValue instanceof String) {
      patternParser((String) newValue);
    }
  }

  /**
   * Parse the input for a pattern like: uri:http://url.com, and set the value (in this case the uri
   * value)
   *
   * @param s
   */
  private void patternParser(String s) {
    String uriString = CommonUtils.extractFieldValue("uri", s);
    String nameString = CommonUtils.extractFieldValue("name", s);
    String longString = CommonUtils.extractFieldValue("long", s);
    String latString = CommonUtils.extractFieldValue("lat", s);
    if (uriString != null) {
      uri = URI.create(uriString);
      externalUri = URI.create(uriString);
    }
    if (nameString != null) {
      this.label = nameString;
      this.name = nameString;
      this.license = nameString;
    }
    if (longString != null) {
      this.longitude = Double.parseDouble(longString);
    }
    if (latString != null) {
      this.latitude = Double.parseDouble(latString);
    }
    toNull = uriString != null || nameString != null || longString != null || latString != null;
    if (!toNull) {
      name = s;
      label = s;
      license = s;
    }
  }

  /**
   * Retun the id (last part of the {@link URI}) of the {@link Statement}. Used for GUI
   * representation
   *
   * @return
   */
  public String getStatementId() {
    return ObjectHelper.getId(getStatement().getId());
  }

  /**
   * JSF listener when a citation has been changed
   */
  public void resetCitationListener(ValueChangeEvent vce) {
    resetCitation = true;
    this.predefinedValueListener(vce);
  }

  /**
   * Reset the citation of a publication
   *
   * @param smb
   */
  public void resetCitation() {
    citation = SearchAndExportHelper.getCitation(uri, exportFormat);
  }

  /**
   * getter for the namespace defining the type of the {@link Metadata}
   *
   * @return
   */
  public String getTypeNamespace() {
    return statement.getType().toString();
  }

  /**
   * getter
   *
   * @return
   */
  public String getText() {
    return text;
  }

  /**
   * setter
   *
   * @param text
   */
  public void setText(String text) {
    if (!toNull) {
      this.text = text;
    }
  }

  /**
   * getter
   *
   * @return
   */
  public Person getPerson() {
    return person;
  }

  /**
   * setter
   *
   * @param person
   */
  public void setPerson(Person person) {
    this.person = person;
  }

  /**
   * getter
   *
   * @return
   */
  public URI getConeId() {
    return coneId;
  }

  /**
   * setter
   *
   * @param coneId
   */
  public void setConeId(URI coneId) {
    this.coneId = coneId;
  }

  /**
   * getter
   *
   * @return
   */
  public URI getUri() {
    return uri;
  }

  /**
   * setter
   *
   * @param uri
   */
  public void setUri(URI uri) {
    if (!toNull) {
      this.uri = uri;
    }
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
  public String getDate() {
    return date;
  }

  /**
   * setter
   *
   * @param date
   */
  public void setDate(String date) {
    if (!toNull) {
      if (date != null && !"".equals(date)) {
        time = DateFormatter.getTime(date);
        this.date = date;
      }
      this.date = date;
    }
  }

  /**
   * getter
   *
   * @return
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * setter
   *
   * @param longitude
   */
  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  /**
   * getter
   *
   * @return
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * setter
   *
   * @param latitude
   */
  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  /**
   * getter
   *
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * setter
   *
   * @param name
   */
  public void setName(String name) {
    if (!toNull) {
      this.name = name;
    }
  }

  /**
   * getter
   *
   * @return
   */
  public String getExportFormat() {
    return exportFormat;
  }

  /**
   * setter
   *
   * @param exportFormat
   */
  public void setExportFormat(String exportFormat) {
    this.exportFormat = exportFormat;
  }

  /**
   * getter
   *
   * @return
   */
  public String getCitation() {
    return citation;
  }

  /**
   * setter
   *
   * @param citation
   */
  public void setCitation(String citation) {
    this.citation = citation;
  }

  /**
   * getter
   *
   * @return
   */
  public double getNumber() {
    return number;
  }

  /**
   * setter
   *
   * @param number
   */
  public void setNumber(double number) {
    if (!toNull) {
      this.number = number;
    }
  }

  /**
   * getter
   *
   * @return
   */
  public String getLicense() {
    return license;
  }

  /**
   * setter
   *
   * @param license
   */
  public void setLicense(String license) {
    if (!toNull) {
      this.license = license;
    }
  }

  /**
   * @return the externalUri
   */
  public URI getExternalUri() {
    return externalUri;
  }

  /**
   * @param externalUri the externalUri to set
   */
  public void setExternalUri(URI externalUri) {
    if (!toNull) {
      this.externalUri = externalUri;
    }
  }

  public void externalURIListener(ValueChangeEvent vce) {
    this.externalUri = (URI) vce.getNewValue();
    toNull = true;
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
  public void setParent(MetadataWrapper parent) {
    this.parent = parent;
  }

  /**
   * getter
   *
   * @return the parent
   */
  public MetadataWrapper getParent() {
    return parent;
  }

  /**
   * getter
   *
   * @return the hierarchyLevel
   */
  public int getHierarchyLevel() {
    return (getTreeIndex().length() - 1) / 2;
  }

  /**
   * getter
   *
   * @return the empty
   */
  public boolean isEmpty() {
    return MetadataHelper.isEmpty(asMetadata());
  }

  /**
   * getter
   *
   * @return the preview
   */
  public boolean isPreview() {
    return preview;
  }

  /**
   * setter
   *
   * @param preview the preview to set
   */
  public void setPreview(boolean preview) {
    this.preview = preview;
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

  public String getLastParentTreeIndex() {
    return treeIndex.split(",", 0)[0];
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(MetadataWrapper o) {
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

  // @Override
  // public boolean equals(Object obj) {
  // if (obj instanceof SuperMetadataBean) {
  // return compareTo((SuperMetadataBean) obj) == 0;
  // }
  // return false;
  // }

  /**
   * Return the higher parent
   *
   * @return
   */
  public MetadataWrapper lastParent() {
    MetadataWrapper smb = this;
    while (smb.getParent() != null) {
      smb = smb.getParent();
    }
    return smb;
  }

  /**
   * @return the childs
   */
  public List<MetadataWrapper> getChilds() {
    return childs;
  }

  /**
   * @param childs the childs to set
   */
  public void setChilds(List<MetadataWrapper> childs) {
    this.childs = childs;
  }

  /**
   * Add emtpy childs according to the {@link MetadataProfile}
   *
   * @param p
   */
  public void addEmtpyChilds(MetadataProfile p) {
    for (Statement st : p.getStatements()) {
      if (st.getParent() != null && st.getParent().compareTo(statement.getId()) == 0) {
        MetadataWrapper child = new MetadataWrapper(MetadataFactory.createMetadata(st), st);
        child.addEmtpyChilds(p);
        childs.add(child);
      }
    }
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
   * @return the customField
   */
  public String getCustomField() {
    return customField;
  }

  /**
   * @param customField the customField to set
   */
  public void setCustomField(String customField) {
    if (!toNull) {
      this.customField = customField;
    }
  }

  /**
   * Add an organization to an author of the {@link CollectionImeji}
   *
   * @param authorPosition
   * @param organizationPosition
   * @return
   */
  public String addOrganization(int organizationPosition) {
    List<Organization> orgs = (List<Organization>) this.person.getOrganizations();
    Organization o = ImejiFactory.newOrganization();
    o.setPos(organizationPosition + 1);
    orgs.add(organizationPosition + 1, o);
    return "";
  }

  /**
   * Remove an organization to an author of the {@link CollectionImeji}
   *
   * @return
   */
  public String removeOrganization(int organizationPosition) {
    List<Organization> orgs = (List<Organization>) this.person.getOrganizations();
    if (orgs.size() > 1) {
      orgs.remove(organizationPosition);
    } else {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_author_need_one_organization",
          BeanHelper.getLocale()));
    }
    return "";
  }
}
