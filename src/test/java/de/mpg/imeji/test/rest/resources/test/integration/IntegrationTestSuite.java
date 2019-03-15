package de.mpg.imeji.test.rest.resources.test.integration;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

import de.mpg.imeji.testimpl.rest.resources.StorageIntegration;
import de.mpg.imeji.testimpl.rest.resources.VersionManagerTest;

@RunWith(Suite.class)
@Suite.SuiteClasses({
    // Tests failing from time to time throwing: 
    //"javax.ws.rs.ProcessingException: Failed to start Grizzly HTTP server: Address already in use: bind"

    // ItemIntegration.class, CollectionIntegration.class,
    StorageIntegration.class, VersionManagerTest.class})

public class IntegrationTestSuite {

}
