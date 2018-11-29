package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.CollectionServiceTest;
import de.mpg.imeji.util.SuperTestSuite;

@RunWith(Suite.class)

@Suite.SuiteClasses({CollectionServiceTest.class /*
                                                  * ShareServiceTest.class, StatementServiceTest.class,
                                                  * UsergroupServiceTest.class, UserServiceTest.class, ContentServiceTest.class
                                                  * 
                                                  * ItemServiceTest.class
                                                  */})

public class ServiceTestSuite extends SuperTestSuite {

}
