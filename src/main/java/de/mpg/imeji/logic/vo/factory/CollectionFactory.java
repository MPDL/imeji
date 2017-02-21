package de.mpg.imeji.logic.vo.factory;

import java.net.URI;

import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;

/**
 * Factory for {@link CollectionImeji}
 *
 * @author saquet
 *
 */
public class CollectionFactory {
  private final CollectionImeji collection = new CollectionImeji();

  public CollectionFactory() {
    // constructor
  }

  public CollectionImeji build() {
    return collection;
  }

  public CollectionFactory setId(String id) {
    collection.setId(URI.create(id));
    return this;
  }

  public CollectionFactory setPerson(Person p) {
    collection.getPersons().add(p);
    return this;
  }

  public CollectionFactory setPerson(String givenName, String familyName, String organization) {
    Person p = new Person();
    p.setFamilyName(familyName);
    p.setGivenName(givenName);
    p.getOrganizations().add(new Organization(organization));
    collection.getPersons().add(p);
    return this;
  }

  public CollectionFactory setTitle(String title) {
    collection.setTitle(title);
    return this;
  }

}
