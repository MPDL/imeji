package de.mpg.imeji.testimpl.logic.service;

import org.apache.log4j.Logger;
import org.junit.BeforeClass;

import de.mpg.imeji.test.logic.service.SuperServiceTest;
import de.mpg.imeji.testimpl.logic.controller.ItemControllerTestClass;

public class ContentServiceTest extends SuperServiceTest {
  private static final Logger LOGGER = Logger.getLogger(ItemControllerTestClass.class);

  @BeforeClass
  public static void specificSetup() {

  }

  // Assuming ItemService.createWithFile will always use ContentService.create, no testing of
  // ContentService.create is necessary

}
