package de.mpg.imeji.testimpl.logic.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.beans.HasPropertyWithValue.hasProperty;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContainerAdditionalInfo;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.UserService.USER_TYPE;
import de.mpg.imeji.test.logic.service.SuperServiceTest;

public class CollectionServiceCreateTest extends SuperServiceTest {

  private static CollectionService collectionService;
  private static User defaultUser;

  private static User sysadmin;

  @BeforeClass
  public static void testSetup() throws ImejiException {
    collectionService = new CollectionService();
    UserService userService = new UserService();

    defaultUser =
        ImejiFactory.newUser().setEmail("defaultUser@test.org").setPerson("defaultName", "defaultFamilyName", "defaultOrganization")
            .setPassword("defaultPassword").setQuota(Long.MAX_VALUE).build();
    userService.create(defaultUser, USER_TYPE.DEFAULT);

    sysadmin = ImejiFactory.newUser().setEmail("admin4@test.org").setPerson("admin4", "admin4", "org").setPassword("password")
        .setQuota(Long.MAX_VALUE).build();
    userService.create(sysadmin, USER_TYPE.ADMIN);
  }

  @Test
  public void createCollectionWithAdditionalInformations() throws ImejiException {
    //Test data
    String label1 = "label1";
    String text1 = "text1";
    String label2 = "label2";
    String text2 = "text2";

    CollectionImeji collectionWithAdditionalInformations =
        ImejiFactory.newCollection().setTitle("Collection").setPerson("Name", "FamilyName", "Organization").build();
    List<ContainerAdditionalInfo> additionalInformations =
        Arrays.asList(new ContainerAdditionalInfo(label1, text1, ""), new ContainerAdditionalInfo(label2, text2, ""));
    collectionWithAdditionalInformations.setAdditionalInformations(additionalInformations);

    //Test
    CollectionImeji createdCollection = collectionService.create(collectionWithAdditionalInformations, defaultUser);

    //Assertion
    defaultUser = new UserService().retrieve(defaultUser.getEmail(), sysadmin);
    List<CollectionImeji> retrievedCollections =
        collectionService.retrieve(Arrays.asList(createdCollection.getId().toString()), defaultUser);
    CollectionImeji retrievedCollection = retrievedCollections.get(0);
    List<ContainerAdditionalInfo> retrievedAdditionalInformations = retrievedCollection.getAdditionalInformations();


    assertThat(retrievedAdditionalInformations, contains(both(hasProperty("label", is(label1))).and(hasProperty("text", is(text1))),
        both(hasProperty("label", is(label2))).and(hasProperty("text", is(text2)))));
    // Using AssertJ Framework, the assertion would be:
    // assertThat(retrievedAdditionalInformations).extracting("label", "text").containsExactly(tuple(label1, text1), tuple(label2, text2));
  }

  @Test
  public void createCollectionWithTypes() throws ImejiException {
    //Test data
    String type1 = "type1";
    String type2 = "type2";

    CollectionImeji collectionWithTypes =
        ImejiFactory.newCollection().setTitle("Collection").setPerson("Name", "FamilyName", "Organization").build();
    List<String> types = Arrays.asList(type1, type2);
    collectionWithTypes.setTypes(types);

    //Test
    CollectionImeji createdCollection = collectionService.create(collectionWithTypes, defaultUser);

    //Assertion
    defaultUser = new UserService().retrieve(defaultUser.getEmail(), sysadmin);
    List<CollectionImeji> retrievedCollections =
        collectionService.retrieve(Arrays.asList(createdCollection.getId().toString()), defaultUser);
    CollectionImeji retrievedCollection = retrievedCollections.get(0);
    List<String> retrievedTypes = retrievedCollection.getTypes();

    assertThat(retrievedTypes, equalTo(types));
    // Using AssertJ Framework, the assertion would be:
    // assertThat(retrievedTypes).isEqualTo(types);
  }

  //Test createMinimalCollection

  //Test createCollectionWithPredefinedTypes

  //Test createCollectionWithPredefinedTypesValidationExceptionNoStudyTypes

  //Test createCollectionWithAdditionalInformationsValidationExceptionWrongORCID

  //Test createCollectionWithAdditionalInformationsValidationExceptionWrongArticleDOI

  //Test createCollectionWithAdditionalInformationsValidationExceptionWrongGeo-coordinates

}
