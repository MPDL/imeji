/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo.predefinedMetadata;

import java.net.URI;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.j2j.annotations.j2jDataType;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.vo.Person;

/**
 * Metadata for {@link Person}. Can have a CoNe identifier
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource(ImejiNamespaces.METADATA)
@j2jDataType("http://imeji.org/terms/metadata#conePerson")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "conePerson", namespace = ImejiNamespaces.METADATA)
@XmlType(propOrder = {"coneId", "person", "statement"})
public class ConePerson extends Metadata {
  private static final long serialVersionUID = -1925520449071527632L;
  @j2jResource("http://xmlns.com/foaf/0.1/person")
  private Person person;
  @j2jResource("http://imeji.org/terms/coneId")
  private URI coneId;

  @j2jResource("http://imeji.org/terms/statement")
  private URI statement;

  public ConePerson() {}

  public ConePerson(Person pers) {
    this.person = pers;
  }

  @XmlElement(name = "person", namespace = "http://xmlns.com/foaf/0.1/")
  public Person getPerson() {
    return person;
  }

  public void setPerson(Person person) {
    this.person = person;
  }

  @XmlElement(name = "coneId", namespace = "http://imeji.org/terms/")
  public URI getConeId() {
    return coneId;
  }

  public void setConeId(URI coneId) {
    this.coneId = coneId;
  }

  @Override
  @XmlElement(name = "statement", namespace = "http://imeji.org/terms/")
  public URI getStatement() {
    return statement;
  }

  @Override
  public void setStatement(URI namespace) {
    this.statement = namespace;
  }

  @Override
  public void copy(Metadata metadata) {
    if (metadata instanceof ConePerson) {
      setPos(metadata.getPos());
      this.person = ((ConePerson) metadata).getPerson().clone();
      this.coneId = ((ConePerson) metadata).getConeId();
      this.statement = ((ConePerson) metadata).getStatement();
    }
  }

  @Override
  public String asFulltext() {
    return person.AsFullText();
  }

  @Override
  public void clean() {
    String completeName = (person.getGivenName() != null ? person.getGivenName() : "")
        + ((person.getGivenName() == null || person.getGivenName().isEmpty()
            || person.getFamilyName() == null || person.getFamilyName().isEmpty()) ? "" : ", ")
        + person.getFamilyName() != null ? person.getFamilyName() : "";
    person.setCompleteName(completeName.trim());
  }
}
