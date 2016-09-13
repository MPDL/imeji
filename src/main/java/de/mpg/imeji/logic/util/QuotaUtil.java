package de.mpg.imeji.logic.util;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.user.controller.UserBusinessController;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;

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

  /**
   * Check user disk space quota. Quota is calculated for user of target collection.
   *
   * @param file
   * @param col
   * @throws ImejiException
   * @return remained disk space after successfully uploaded <code>file</code>; <code>-1</code> will
   *         be returned for unlimited quota
   */
  public static long checkQuota(User user, File file, CollectionImeji col) throws ImejiException {
    // do not check quota for admin
    if (col == null) {
      return -1L;
    }
    User targetCollectionUser = user.getId().equals(col.getCreatedBy()) ? user
        : new UserBusinessController().retrieve(col.getCreatedBy(), Imeji.adminUser);

    Search search = SearchFactory.create();
    List<String> results =
        search.searchString(JenaCustomQueries.selectUserFileSize(col.getCreatedBy().toString()),
            null, null, 0, -1).getResults();
    long currentDiskUsage = 0L;
    try {
      currentDiskUsage = Long.parseLong(results.get(0).toString());
    } catch (NumberFormatException e) {
      throw new UnprocessableError("Cannot parse currentDiskSpaceUsage " + results.get(0).toString()
          + "; requested by user: " + user.getEmail() + "; targetCollectionUser: "
          + targetCollectionUser.getEmail(), e);
    }
    long needed = currentDiskUsage + file.length();
    if (needed > targetCollectionUser.getQuota()) {
      throw new QuotaExceededException("Data quota ("
          + QuotaUtil.getQuotaHumanReadable(targetCollectionUser.getQuota(), Locale.ENGLISH)
          + " allowed) has been exceeded (" + FileUtils.byteCountToDisplaySize(currentDiskUsage)
          + " used)");
    }
    return targetCollectionUser.getQuota() - needed;
  }
}
