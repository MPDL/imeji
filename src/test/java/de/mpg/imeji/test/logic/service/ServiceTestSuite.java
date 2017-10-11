package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.CollectionServiceTest;
import de.mpg.imeji.testimpl.logic.service.ContentServiceTest;
import de.mpg.imeji.testimpl.logic.service.ItemServiceTest;
import de.mpg.imeji.testimpl.logic.service.ShareServiceTest;
import de.mpg.imeji.testimpl.logic.service.StatementServiceTest;
import de.mpg.imeji.testimpl.logic.service.UserServiceTest;
import de.mpg.imeji.testimpl.logic.service.UsergroupServiceTest;
import de.mpg.imeji.util.SuperTestSuite;



@RunWith(Suite.class)



@Suite.SuiteClasses({CollectionServiceTest.class, ShareServiceTest.class,
    StatementServiceTest.class, UsergroupServiceTest.class, UserServiceTest.class,
    ItemServiceTest.class, ContentServiceTest.class})


public class ServiceTestSuite extends SuperTestSuite {

}
