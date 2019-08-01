package de.mpg.imeji.test.logic.controller;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.logic.businesscontroller.InvitationBusinessControllerTest;
import de.mpg.imeji.testimpl.logic.businesscontroller.RegistrationBusinessControllerTest;
import de.mpg.imeji.testimpl.logic.controller.StatisticsControllerTestClass;

@RunWith(Suite.class)
@Suite.SuiteClasses({InvitationBusinessControllerTest.class, RegistrationBusinessControllerTest.class, StatisticsControllerTestClass.class})

public class ControllerTestSuite {

}
