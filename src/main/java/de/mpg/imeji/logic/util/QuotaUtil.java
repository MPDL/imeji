package de.mpg.imeji.logic.util;

import org.apache.log4j.Logger;

import de.mpg.imeji.presentation.beans.ConfigurationBean;

/**
 * Utility Class for imeji Quota
 * 
 * @author bastiens
 *
 */
public class QuotaUtil {
  private static final int BYTES_PER_GB = 1073741824;
  private static final Logger LOGGER = Logger.getLogger(QuotaUtil.class);

  private QuotaUtil() {
    // private constructor
  }

  /**
   * Return a Quota defined in GB
   * 
   * @param gigaByte
   * @return
   */
  public static long getQuotaInBytes(String gigaByte) {
    try {
      if (ConfigurationBean.QUOTA_UNLIMITED.equals(gigaByte)) {
        return Long.MAX_VALUE;
      }
      return (long) ((Double.parseDouble(gigaByte)) * BYTES_PER_GB);
    } catch (Exception e) {
      LOGGER.error("Error parsing quota: ", e);
      return 0;
    }
  }
}
