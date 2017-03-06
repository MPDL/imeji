package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.DummyTest;
import de.mpg.imeji.testimpl.logic.service.ItemServiceTest;
import util.SuperTestSuite;



@RunWith(Suite.class)
@Suite.SuiteClasses({DummyTest.class, ItemServiceTest.class})
public class ServiceTestSuite extends SuperTestSuite {

}
