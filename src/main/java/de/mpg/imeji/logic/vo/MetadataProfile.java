/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;

/**
 * Profile where {@link Item} {@link Metadata} are defined
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/mdprofile")
@j2jId(getMethod = "getId", setMethod = "setId")
@j2jModel("metadataProfile")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "mdprofile", namespace = "http://imeji.org/terms/")
public class MetadataProfile extends Properties implements Cloneable {
  private static final long serialVersionUID = -3303333109346078736L;
  @j2jLiteral("http://purl.org/dc/elements/1.1/title")
  private String title;
  @j2jLiteral("http://purl.org/dc/elements/1.1/description")
  private String description;
  @j2jList("http://imeji.org/terms/statement")
  private Collection<Statement> statements = new ArrayList<Statement>();

  @j2jLiteral("http://imeji.org/terms/default")
  private boolean def = false;

  @XmlElement(name = "title", namespace = "http://purl.org/dc/elements/1.1/")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @XmlElement(name = "description", namespace = "http://purl.org/dc/elements/1.1/")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  @XmlElement(name = "statement", namespace = "http://imeji.org/terms/")
  public Collection<Statement> getStatements() {
    return statements;
  }

  public void setStatements(Collection<Statement> statements) {
    this.statements = statements;
  }

  @XmlElement(name = "default", namespace = "http://imeji.org/terms/")
  public boolean getDefault() {
    return def;
  }

  public void setDefault(boolean def) {
    this.def = def;
  }

  /*
   * (non-Javadoc)
   *
   * @see java.lang.Object#clone()
   */
  @Override
  public MetadataProfile clone() {
    MetadataProfile clone = new MetadataProfile();
    clone.setTitle(this.title);
    clone.setDescription(this.description);
    // the mapping between the new uris (created by cloning) and the old uris
    Map<String, URI> idMapping = new HashMap<String, URI>();
    for (Statement s : statements) {
      Statement c = s.clone();
      clone.getStatements().add(c);
      idMapping.put(s.getId().toString(), c.getId());
    }
    // Set the new parent
    for (Statement s : clone.statements) {
      if (s.getParent() != null) {
        s.setParent(idMapping.get(s.getParent().toString()));
      }
    }

    return clone;
  }

  public Statement getStatement(String statementId) {
    for (Statement st : statements) {
      if (statementId.equals(st.getId().toString())) {
        return st;
      }
    }
    return null;
  }

}
