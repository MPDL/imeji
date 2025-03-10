package de.mpg.imeji.logic.db.repositories;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.model.*;

import java.util.List;

public class CollectionsDbRepository extends DbRepository<CollectionImeji> {

  public CollectionsDbRepository() {
    super(CollectionImeji.class);
  }


  public List<CollectionImeji> retrieveAllSubCollections() throws ImejiException {
    return inSession(em -> {
      return em.createQuery("select i from CollectionImeji i WHERE i.collection IS NOT NULL", CollectionImeji.class).getResultList();
    });
  }

  public List<CollectionImeji> retrieveCollectionsByLogoUrl(String filePath) throws ImejiException {
    return inSession(em -> {
      return em.createNativeQuery("SELECT * FROM Collection c WHERE c.logoUrl ILIKE :filePath", CollectionImeji.class)
          .setParameter("filePath", "%" + filePath + "%").getResultList();
    });
  }

  public List<Person> retrievePersonsbyId(String personId) throws ImejiException {
    return inSession(em -> {
      //String adminRule = "ADMIN," + Imeji.PROPERTIES.getBaseURI();
      List<CollectionImeji> userWithPersons =  em.createNativeQuery("SELECT DISTINCT collection.* FROM collection, jsonb_array_elements(persons) AS p WHERE p ->> 'id' = personId;", CollectionImeji.class)
              .setParameter("personId", "personId")
              .getResultList();
      return userWithPersons.stream().flatMap(u -> u.getPersons().stream().filter(p-> personId.equals(p.getId().toString()))).toList();
    });
  }
}
