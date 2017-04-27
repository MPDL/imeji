package de.mpg.imeji.test.logic.service;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.service.ShareServiceTest;
import de.mpg.imeji.util.SuperTestSuite;



@RunWith(Suite.class)
@Suite.SuiteClasses({ShareServiceTest.class})
public class ServiceTestSuite extends SuperTestSuite {

}
