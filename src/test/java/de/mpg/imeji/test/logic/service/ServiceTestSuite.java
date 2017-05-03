package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.StatementServiceTest;
import de.mpg.imeji.util.SuperTestSuite;



@RunWith(Suite.class)
/*
 * @Suite.SuiteClasses({ShareServiceTest.class, StatementServiceTest.class,
 * UsergroupServiceTest.class, UserServiceTest.class, ItemServiceTest.class,
 * CollectionServiceTest.class, ContentServiceTest.class})
 */
@Suite.SuiteClasses({StatementServiceTest.class})
public class ServiceTestSuite extends SuperTestSuite {

}
