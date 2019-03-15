package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.ContentServiceTest;
import de.mpg.imeji.testimpl.logic.service.ShareServiceTest;
import de.mpg.imeji.testimpl.logic.service.StatementServiceTest;
import de.mpg.imeji.testimpl.logic.service.UserServiceTest;
import de.mpg.imeji.testimpl.logic.service.UsergroupServiceTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    //CollectionServiceTest.class, 
    ShareServiceTest.class, StatementServiceTest.class, UsergroupServiceTest.class, UserServiceTest.class, ContentServiceTest.class,
    //ItemServiceTest.class
})

public class ServiceTestSuite {

}
