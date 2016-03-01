package de.mpg.imeji.testimpl.logic.controller;


import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_REST;
import static de.mpg.imeji.test.rest.resources.test.integration.MyTestContainerFactory.STATIC_CONTEXT_STORAGE;
import static org.junit.Assert.assertEquals;

import java.io.File;

import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;

import org.junit.Test;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.StatisticsController;
import de.mpg.imeji.rest.api.CollectionService;
import de.mpg.imeji.rest.api.DefaultItemService;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;
import util.JenaUtil;

public class StatisticsControllerTestClass extends ImejiTestBase {

  @Test
  public void test_3_ReleaseCollection_1_WithAuth() throws ImejiException {
    long totalFileSize = 0;
    DefaultItemService service = new DefaultItemService();
    initCollection();
    initItem("test"); // +1
    totalFileSize += service.read(itemId, JenaUtil.testUser).getFileSize();
    initItem("test2"); // +2
    totalFileSize += service.read(itemId, JenaUtil.testUser).getFileSize();
    initItem("test3"); // +3
    long lastAddedItemSize = service.read(itemId, JenaUtil.testUser).getFileSize();
    totalFileSize += service.read(itemId, JenaUtil.testUser).getFileSize();

    // deleteItem
    Form form = new Form();
    form.param("id", itemId);
    target("/rest/items").register(authAsUser).path("/" + itemId)
        .request(MediaType.APPLICATION_JSON_TYPE).delete(); // +2

    totalFileSize -= lastAddedItemSize;
    // init Collection with testUser2
    CollectionService cs = new CollectionService();
    try {
      collectionTO = (CollectionTO) RestProcessUtils.buildTOFromJSON(
          getStringFromPath(STATIC_CONTEXT_REST + "/createCollection.json"), CollectionTO.class);
      collectionTO = cs.create(collectionTO, JenaUtil.testUser2);
      collectionId = collectionTO.getId();
    } catch (Exception e) {
    }

    // init Item with testUser2
    DefaultItemWithFileTO to = new DefaultItemWithFileTO();
    to.setCollectionId(collectionId);
    to.setFile(new File(STATIC_CONTEXT_STORAGE + "/test4.jpg"));
    to.setStatus("PENDING");
    try {
      itemTO = service.create(to, JenaUtil.testUser2); // +3
      itemId = itemTO.getId();
    } catch (Exception e) {
      // Do nothing
    }

    StatisticsController controller = new StatisticsController();
    long result = controller.getUsedStorageSizeForInstitute("imeji.org");

    assertEquals(service.read(itemId, JenaUtil.testUser2).getFileSize() + totalFileSize, result);

  }

}
