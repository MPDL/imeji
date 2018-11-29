package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.PersonTO;
import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;

@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#conePerson")
@XmlType(propOrder = {"person",})
public class ConePersonTO extends MetadataTO {
  private static final long serialVersionUID = -7123511172438572514L;
  private PersonTO person;

  public PersonTO getPerson() {
    return person;
  }

  public void setPerson(PersonTO person) {
    this.person = person;
  }
}
