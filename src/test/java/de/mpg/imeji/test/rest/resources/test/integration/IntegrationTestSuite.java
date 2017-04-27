package de.mpg.imeji.test.rest.resources.test.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.rest.resources.CollectionIntegration;
import de.mpg.imeji.testimpl.rest.resources.ItemIntegration;
import de.mpg.imeji.testimpl.rest.resources.StorageIntegration;
import de.mpg.imeji.testimpl.rest.resources.VersionManagerTest;
import de.mpg.imeji.util.SuperTestSuite;


@RunWith(Suite.class)
@Suite.SuiteClasses({ItemIntegration.class, CollectionIntegration.class, StorageIntegration.class,
    VersionManagerTest.class})


public class IntegrationTestSuite extends SuperTestSuite {

}
