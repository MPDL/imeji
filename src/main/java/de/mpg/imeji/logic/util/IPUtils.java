package de.mpg.imeji.logic.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.apache.log4j.Logger;

import com.google.common.net.InetAddresses;

/**
 * Utility Classs for IP operations
 *
 * @author saquet
 *
 */
public class IPUtils {
  private static final Logger LOGGER = Logger.getLogger(IPUtils.class);

  /**
   * Private Constructor
   */
  private IPUtils() {
    // Avoid creation
  }

  /**
   * True if the ip is include into the IP Range. IP range can be: <br/>
   * 130.183.100-129.* <br/>
   * 130.183.248.12-13 <br/>
   * 85.183.114.142 <br/>
   * 141.61.*.* <br/>
   *
   * @param ipRange
   * @param ip
   * @return
   * @throws UnknownHostException
   */
  public static boolean isInRange(String ipRange, String ip) {

    try {
      return ipToLong(InetAddress.getByName(ip)) >= ipToLong(getMinIP(ipRange))
          && ipToLong(InetAddress.getByName(ip)) <= ipToLong(getMaxIP(ipRange));
    } catch (final UnknownHostException e) {
      LOGGER.error(e.getMessage());
    }
    return false;
  }

  /**
   * Return the minimal IP of an IP range
   *
   * @param ipRangeString
   * @return
   * @throws UnknownHostException
   */
  public static InetAddress getMinIP(String ipRangeString) throws UnknownHostException {
    String ip = "";
    for (final String s : ipRangeString.split("\\.")) {
      if (!"".equals(ip)) {
        ip += ".";
      }
      if ("*".equals(s)) {
        ip += "0";
      } else if (s.contains("-")) {
        ip += s.split("-")[0];
      } else {
        ip += s;
      }
    }
    if (InetAddresses.isInetAddress(ip.trim())) {
      return InetAddresses.forString(ip.trim());
    }
    return InetAddress.getLocalHost();
  }

  /**
   * Return the maximal IP of an IP range
   *
   * @param ipRangeString
   * @return
   * @throws UnknownHostException
   */
  public static InetAddress getMaxIP(String ipRangeString) throws UnknownHostException {
    String ip = "";
    for (final String s : ipRangeString.split("\\.")) {
      if (!"".equals(ip)) {
        ip += ".";
      }
      if ("*".equals(s)) {
        ip += "255";
      } else if (s.contains("-")) {
        ip += s.split("-")[1];
      } else {
        ip += s;
      }
    }
    if (InetAddresses.isInetAddress(ip.trim())) {
      return InetAddresses.forString(ip.trim());
    }
    return InetAddress.getLocalHost();
  }

  /**
   * Convert an ip to a long with can be then compared to another
   *
   * @param ip
   * @return
   */
  public static long ipToLong(InetAddress ip) {
    final byte[] octets = ip.getAddress();
    long result = 0;
    for (final byte octet : octets) {
      result <<= 8;
      result |= octet & 0xff;
    }
    return result;
  }
}
