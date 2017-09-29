package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * a foaf person
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://xmlns.com/foaf/0.1/person")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Person implements Cloneable, Serializable {
  private static final long serialVersionUID = 2030269396417009337L;
  private URI id;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/family-name")
  private String familyName;
  @j2jLiteral("http://purl.org/escidoc/metadata/terms/0.1/given-name")
  private String givenName;
  @j2jLiteral("http://purl.org/dc/elements/1.1/identifier")
  private String identifier;
  @j2jLiteral("http://imeji.org/terms/position")
  private int pos = 0;
  @j2jList("http://purl.org/escidoc/metadata/profiles/0.1/organizationalunit")
  protected Collection<Organization> organizations = new ArrayList<Organization>();

  public Person() {
    this.id = IdentifierUtil.newURI(Person.class);
  }

  public String getFamilyName() {
    return familyName;
  }

  public void setFamilyName(String familyName) {
    this.familyName = familyName;
  }

  public String getGivenName() {
    return givenName;
  }

  public void setGivenName(String givenName) {
    this.givenName = givenName;
  }

  public String getIdentifier() {
    return identifier;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public Collection<Organization> getOrganizations() {
    return organizations;
  }

  public void setOrganizations(Collection<Organization> organizations) {
    this.organizations = organizations;
  }

  public int getPos() {
    return pos;
  }

  public void setPos(int pos) {
    this.pos = pos;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  public String getOrganizationString() {
    String s = "";
    for (final Organization o : organizations) {
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
    String str = givenName + " " + familyName + " ";
    for (final Organization org : organizations) {
      str += " " + org.getName();
    }
    return str.trim();
  }

  @Override
  public Person clone() {
    final Person clone = new Person();
    clone.familyName = this.familyName;
    clone.givenName = this.givenName;
    if (identifier != null && !"".equals(identifier)) {
      clone.identifier = this.identifier;
    }
    for (final Organization org : this.organizations) {
      clone.organizations.add(org.clone());
    }
    clone.pos = this.pos;
    return clone;
  }

  public String getCompleteName() {
    return familyName + ", " + givenName;
  }

  public String getFirstnameLastname() {
    return givenName + " " + familyName;
  }
}
