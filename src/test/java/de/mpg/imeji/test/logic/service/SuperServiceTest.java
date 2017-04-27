package de.mpg.imeji.test.logic.service;

import org.junit.AfterClass;
import org.junit.BeforeClass;

import de.mpg.imeji.util.JenaUtil;

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
