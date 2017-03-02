package de.mpg.imeji.testimpl.logic.service;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.content.ContentService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.ImejiTestResources;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;
import util.JenaUtil;

public class ContentServiceTest extends SuperServiceTest {
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
    ContentService service = new ContentService();
    ItemService itemService = new ItemService();
    try {
      Item item2 = ImejiFactory.newItem(collectionBasic);
      itemService.create(item2, collectionBasic, JenaUtil.testUser);
      ContentVO content =
          service.create(item2, ImejiTestResources.getTest2Jpg(), JenaUtil.testUser);
      Assert.assertEquals("Content should have right ID of Item", item2.getId().toString(),
          content.getItemId());

    } catch (ImejiException e) {
      Assert.fail(e.getMessage());
    }
  }

}
