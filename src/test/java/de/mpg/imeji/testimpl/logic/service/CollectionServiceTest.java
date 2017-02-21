package de.mpg.imeji.testimpl.logic.service;

import java.util.ArrayList;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Grant;
import de.mpg.imeji.logic.vo.Grant.GrantType;
import de.mpg.imeji.logic.vo.factory.CollectionFactory;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;
import util.JenaUtil;

public class CollectionServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ItemControllerTestClass.class);


  @BeforeClass
  public static void specificSetup() {
    try {
      createCollection();
      createItemWithFile();
    } catch (ImejiException e) {
      LOGGER.error("Error initializing collection or item", e);
    }
  }

  @Test
  public void create() {
    CollectionService service = new CollectionService();
    CollectionFactory factory = ImejiFactory.newCollection();
    factory.setPerson("Plack", "Max", "MPDL");
    factory.setTitle("Test Collection");
    CollectionImeji collection = factory.build();
    try {
      collection = service.create(collection, JenaUtil.testUser);
      CollectionImeji retrieved = service.retrieve(collection.getId(), JenaUtil.testUser);
      Assert.assertEquals("Retrieved collection should have the title of the old collection",
          "Test Collection", retrieved.getTitle());
      String grant = ((ArrayList<String>) JenaUtil.testUser.getGrants()).get(3);
      Assert.assertEquals("User should have grant for the new collection", true,
          JenaUtil.testUser.getGrants()
              .contains(new Grant(GrantType.ADMIN, collection.getId().toString()).toGrantString()));
    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }
}
