package de.mpg.imeji.util;

import java.net.UnknownHostException;

import org.junit.Assert;
import org.junit.Test;

import de.mpg.imeji.logic.util.IPUtils;

/**
 * Test the {@link IPUtils} mehtods
 * 
 * @author saquet
 *
 */
public class IPUtilsTest {

  @Test
  public void isInRange() {
    Assert.assertTrue(IPUtils.isInRange("192.129.1.*", "192.129.1.102"));
    Assert.assertTrue(IPUtils.isInRange("192.129.1.100-102", "192.129.1.102"));
    Assert.assertTrue(IPUtils.isInRange("192.129.1.100-102", "192.129.1.100"));
    Assert.assertTrue(IPUtils.isInRange("192.129.*.100-102", "192.129.1.102"));
    Assert.assertFalse(IPUtils.isInRange("192.129.1.*", "192.129.2.102"));
    Assert.assertFalse(IPUtils.isInRange("192.129.1.103-105", "192.129.1.102"));
  }

  @Test
  public void getMinIpOfRange() throws UnknownHostException {
    Assert.assertTrue("192.129.1.0".equals(IPUtils.getMinIP("192.129.1.*").getHostAddress()));
    Assert.assertTrue("192.129.1.100".equals(IPUtils.getMinIP("192.129.1.100-102").getHostAddress()));
  }

  @Test
  public void getMaxIpOfRange() throws UnknownHostException {
    Assert.assertTrue("192.129.1.255".equals(IPUtils.getMaxIP("192.129.1.*").getHostAddress()));
    Assert.assertTrue("192.129.1.102".equals(IPUtils.getMaxIP("192.129.1.100-102").getHostAddress()));
  }
}
