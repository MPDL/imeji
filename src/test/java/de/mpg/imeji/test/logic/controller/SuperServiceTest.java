package de.mpg.imeji.test.logic.controller;

import java.io.File;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.collection.CollectionService.MetadataProfileCreationMethod;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.testimpl.ImejiTestResources;
import util.JenaUtil;

/**
 * Created by vlad on 15.04.15.
 */
public class SuperServiceTest {

  protected static CollectionImeji collection = null;
  protected static Item item = null;

  @BeforeClass
  public static void setup() {
    JenaUtil.initJena();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    JenaUtil.closeJena();
  }

  /**
   * Create collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static CollectionImeji createCollection() throws ImejiException {
    CollectionService controller = new CollectionService();
    collection = ImejiFactory.newCollection("test", "Planck", "Max", "MPG");
    return controller.create(collection, JenaUtil.testUser, MetadataProfileCreationMethod.COPY,
        null);
  }

  /**
   * Create Item in current collection with JenaUtil.testUser
   * 
   * @return
   * @throws ImejiException
   */
  protected static Item createItem() throws ImejiException {
    ItemService controller = new ItemService();
    item = controller.create(ImejiFactory.newItem(collection), collection, JenaUtil.testUser);
    return item;
  }

  protected static Item createItemWithFile() throws ImejiException {
    return createItemWithFile(getOriginalfile());
  }

  protected static Item createItemWithFile(File file) throws ImejiException {
    if (collection == null) {
      createCollection();
    }
    return createItemWithFile(file, collection, JenaUtil.testUser);
  }

  protected static Item createItemWithFile(File file, CollectionImeji collection, User user)
      throws ImejiException {
    ItemService controller = new ItemService();
    item = ImejiFactory.newItem(collection);
    item = controller.createWithFile(item, file, file.getName(), collection, user);
    return item;
  }

  /**
   * @return the originalfile
   */
  protected static File getOriginalfile() {
    return ImejiTestResources.getTestJpg();
  }

  /**
   * @return the thumbnailfile
   */
  protected static File getThumbnailfile() {
    return ImejiTestResources.getTestPng();
  }
}
