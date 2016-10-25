/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jList;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * Metadata of a {@link Container} TODO Description
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/container/metadata")
@j2jId(getMethod = "getId", setMethod = "setId")
public class ContainerMetadata implements Serializable {
  private static final long serialVersionUID = -1323309830123608910L;
  @j2jLiteral("http://purl.org/dc/elements/1.1/title")
  private String title;
  @j2jLiteral("http://purl.org/dc/elements/1.1/description")
  private String description;
  @j2jList("http://xmlns.com/foaf/0.1/person")
  protected Collection<Person> persons = new ArrayList<Person>();
  private URI id = IdentifierUtil.newURI(ContainerMetadata.class);
  @j2jList("http://imeji.org/AdditionalInfo")
  private List<ContainerAdditionalInfo> additionalInformations = new ArrayList<>();

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public Collection<Person> getPersons() {
    return persons;
  }

  public void setPersons(Collection<Person> person) {
    this.persons = person;
  }

  public void setId(URI id) {
    this.id = id;
  }

  public URI getId() {
    return id;
  }

  /**
   * @return the additionalInformations
   */
  public List<ContainerAdditionalInfo> getAdditionalInformations() {
    return additionalInformations;
  }

  /**
   * @param additionalInformations the additionalInformations to set
   */
  public void setAdditionalInformations(List<ContainerAdditionalInfo> additionalInformations) {
    this.additionalInformations = additionalInformations;
  }
}
