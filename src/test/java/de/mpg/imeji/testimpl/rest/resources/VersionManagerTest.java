package de.mpg.imeji.testimpl.rest.resources;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.rest.ImejiRestService;
import de.mpg.imeji.rest.version.VersionManager;
import de.mpg.imeji.rest.version.exception.DeprecatedAPIVersionException;
import de.mpg.imeji.rest.version.exception.UnknowAPIVersionException;
import de.mpg.imeji.test.rest.resources.test.integration.ImejiTestBase;

/**
 * Test the {@link VersionManager}
 * 
 * @author saquet
 *
 */
public class VersionManagerTest extends ImejiTestBase {

  private static String pathPrefixWithVersion = "/rest/vXXX_VERSION_XXX/collections";
  private static String pathPrefix = "/rest/collections";


  @Test
  public void testDeprecatedVersion() throws IOException {
    String version = Integer.toString(ImejiRestService.CURRENT_VERSION - 1);
    VersionManager versionManager = new VersionManager();
    try {
      versionManager.checkVersion(pathPrefixWithVersion.replace("XXX_VERSION_XXX", version));
      Assert.fail("Version should be deprecated");
    } catch (UnknowAPIVersionException e) {
      Assert.fail("Wrong error for deprecated version number");
    } catch (DeprecatedAPIVersionException e) {
      // Ok
    }
  }

  @Test
  public void testUnknownVersion() {
    String version = Integer.toString(ImejiRestService.CURRENT_VERSION + 1);
    VersionManager versionManager = new VersionManager();
    try {
      versionManager.checkVersion(pathPrefixWithVersion.replace("XXX_VERSION_XXX", version));
      Assert.fail("API Version should be unknown");
    } catch (UnknowAPIVersionException e) {
      // Ok
    } catch (DeprecatedAPIVersionException e) {
      Assert.fail("Wrong error for unknown version number");
    }
  }

  @Test
  public void testcurrentVersion() {
    String version = Integer.toString(ImejiRestService.CURRENT_VERSION);
    VersionManager versionManager = new VersionManager();
    try {
      versionManager.checkVersion(pathPrefixWithVersion.replace("XXX_VERSION_XXX", version));
    } catch (UnknowAPIVersionException e) {
      Assert.fail("API Version should not be unknown");
    } catch (DeprecatedAPIVersionException e) {
      Assert.fail("API Version should not be deprecated");
    }
  }

  @Test
  public void testlatestVersion() {
    VersionManager versionManager = new VersionManager();
    try {
      versionManager.checkVersion(pathPrefix);
    } catch (UnknowAPIVersionException e) {
      Assert.fail("API Version should not be unknown");
    } catch (DeprecatedAPIVersionException e) {
      Assert.fail("API Version should not be deprecated");
    }
  }

}
