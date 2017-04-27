package de.mpg.imeji.test.logic.service;

import org.junit.AfterClass;
import org.junit.BeforeClass;

<<<<<<< HEAD
import util.JenaUtil;
=======
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.CollectionFactory;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.util.ImejiTestResources;
import de.mpg.imeji.util.JenaUtil;
>>>>>>> branch 'imeji4' of https://github.com/MPDL/imeji.git

/**
 * Created by vlad on 15.04.15.
 */
public class SuperServiceTest {
  @BeforeClass
  public static void setup() {
    JenaUtil.initJena();
  }

  @AfterClass
  public static void tearDown() throws Exception {
    JenaUtil.closeJena();
  }
}
