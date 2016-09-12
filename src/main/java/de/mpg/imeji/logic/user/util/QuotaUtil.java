package de.mpg.imeji.logic.user.util;

import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;

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
      if (ImejiConfiguration.QUOTA_UNLIMITED.equals(gigaByte)) {
        return Long.MAX_VALUE;
      }
      return (long) ((Double.parseDouble(gigaByte)) * BYTES_PER_GB);
    } catch (Exception e) {
      LOGGER.error("Error parsing quota: ", e);
      return 0;
    }
  }

  /**
   * Return a Quota in Bytes as a user friendly value
   * 
   * @param quota
   * @param locale
   * @return
   */
  public static String getQuotaHumanReadable(long quota, Locale locale) {
    if (quota == Long.MAX_VALUE) {
      return Imeji.RESOURCE_BUNDLE.getLabel("unlimited", locale);
    } else {
      return FileUtils.byteCountToDisplaySize(quota);
    }
  }
}
