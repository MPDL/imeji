package de.mpg.imeji.logic.model.factory;

import java.net.URI;

import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Organization;
import de.mpg.imeji.logic.model.Person;

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

  public CollectionFactory setCollection(String collectionUri) {
    collection.setCollection(URI.create(collectionUri));
    return this;
  }

}
