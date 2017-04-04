package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.CollectionServiceTest;
import util.SuperTestSuite;



@RunWith(Suite.class)
@Suite.SuiteClasses({CollectionServiceTest.class})
public class ServiceTestSuite extends SuperTestSuite {

}
