/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElements;
import javax.xml.bind.annotation.XmlRootElement;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.vo.util.ObjectHelper;

/**
 * a foaf person
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://xmlns.com/foaf/0.1/person")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "person", namespace = "http://xmlns.com/foaf/0.1/")
public class Person implements Cloneable, Serializable {
  private static final long serialVersionUID = 2030269396417009337L;
  private URI id;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/family-name")
  private String familyName;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/given-name")
  private String givenName;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/complete-name")
  private String completeName;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/alternative-name")
  private String alternativeName;
  @j2jLiteral("http://purl.org/dc/elements/1.1/identifier")
  private String identifier;
  @j2jResource("http://purl.org/escidoc/metadata/terms/0.1/role")
  private URI role;
  @j2jLiteral("http://imeji.org/terms/position")
  private int pos = 0;
  @j2jList("http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit")
  protected Collection<Organization> organizations = new ArrayList<Organization>();

  public Person() {
    this.id = IdentifierUtil.newURI(Person.class);
    this.identifier = ObjectHelper.getId(id);
  }

  @XmlElement(name = "family-name", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
    setCompleteName(this.givenName, this.familyName);
  }

  @XmlElement(name = "given-name", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
    setCompleteName(this.givenName, this.familyName);
  }

  protected void setCompleteName(String familyName, String givenName) {
    this.completeName = givenName
        + ((givenName == null || givenName.isEmpty() || familyName == null || familyName.isEmpty())
            ? "" : ", ")
        + familyName;
    this.completeName = completeName.trim();
  }

  @XmlElement(name = "alternative-name", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getAlternativeName() {
    return alternativeName;
  }

  public void setAlternativeName(String alternativeName) {
    this.alternativeName = alternativeName;
  }

  @XmlElement(name = "identifier", namespace = "http://purl.org/dc/elements/1.1/")
  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  @XmlElement(name = "role", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public URI getRole() {
    return role;
  }

  public void setRole(URI role) {
    this.role = role;
  }

  @XmlElements(value = @XmlElement(name = "organizationalunit",
      namespace = "http://purl.org/escidoc/metadata/profiles/0.1/") )
  public Collection<Organization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(Collection<Organization> organizations) {
    this.organizations = organizations;
  }

  @XmlElement(name = "complete-name", namespace = "http://purl.org/escidoc/metadata/terms/0.1/")
  public String getCompleteName() {
    return completeName;
  }

  public void setCompleteName(String completeName) {
    this.completeName = completeName;
  }

  public int getPos() {
    return pos;
  }

  @XmlElement(name = "position", namespace = "http://imeji.org/terms/")
  public void setPos(int pos) {
    this.pos = pos;
  }

  public void setId(URI id) {
    this.id = id;
  }

  @XmlAttribute(name = "id")
  public URI getId() {
    return id;
  }

  public String getOrganizationString() {
    String s = "";
    for (Organization o : organizations) {
      if (!"".equals(s)) {
        s += " ,";
      }
      s += o.getName();
    }
    return s;
  }

  /**
   * The full text to search for this person
   *
   * @return
   */
  public String AsFullText() {
    String str = givenName + " " + familyName + " " + alternativeName;
    for (Organization org : organizations) {
      str += " " + org.getName();
    }
    return str.trim();
  }

  @Override
  public Person clone() {
    Person clone = new Person();
    clone.alternativeName = this.alternativeName;
    clone.completeName = this.completeName;
    clone.familyName = this.familyName;
    clone.givenName = this.givenName;
    if (identifier != null && !"".equals(identifier)) {
      clone.identifier = this.identifier;
    }
    for (Organization org : this.organizations) {
      clone.organizations.add(org.clone());
    }
    clone.role = this.role;
    clone.pos = this.pos;
    return clone;
  }
}
