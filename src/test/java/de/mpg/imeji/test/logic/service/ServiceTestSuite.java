package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.CollectionServiceTest;
import de.mpg.imeji.testimpl.logic.service.ItemServiceTest;
import util.SuperTestSuite;



@RunWith(Suite.class)
@Suite.SuiteClasses({ItemServiceTest.class, CollectionServiceTest.class})
public class ServiceTestSuite extends SuperTestSuite {

}
