package de.mpg.imeji.logic.security.user.util;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.apache.commons.io.FileUtils;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.security.user.UserService;

/**
 * Utility Class for imeji Quota
 *
 * @author bastiens
 *
 */
public class QuotaUtil {
	private static final int BYTES_PER_GB = 1073741824;
	private static final Logger LOGGER = LogManager.getLogger(QuotaUtil.class);

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
		} catch (final Exception e) {
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
	 * Check user disk space quota. Quota is calculated for user of target
	 * collection.
	 *
	 * @param file
	 * @param col
	 * @throws ImejiException
	 * @return remained disk space after successfully uploaded <code>file</code>;
	 *         <code>-1</code> will be returned for unlimited quota
	 */
	public static long checkQuota(User user, File file, CollectionImeji col) throws ImejiException {
		User targetCollectionUser = null;
		try {
			targetCollectionUser = col == null || user.getId().equals(col.getCreatedBy())
					? user
					: new UserService().retrieve(col.getCreatedBy(), Imeji.adminUser);
		} catch (NotFoundException e) {
			throw new UnprocessableError("Collection owner unknown: Cannot check quota. Please contact your admin", e);
		}

		long currentDiskUsage = 0L;
		try {
			currentDiskUsage = getUsedQuota(user);
		} catch (final NumberFormatException e) {
			throw new UnprocessableError("Cannot parse currentDiskSpaceUsage " + "; requested by user: "
					+ user.getEmail() + "; targetCollectionUser: " + targetCollectionUser.getEmail(), e);
		}
		return checkQuota(currentDiskUsage, targetCollectionUser, file, col);
	}

	/**
	 * Check user disk space quota. Quota is calculated for user of target
	 * collection.
	 * 
	 * @param currentDiskUsage
	 * @param user
	 * @param file
	 * @param col
	 * @return
	 * @throws ImejiException
	 */
	public static long checkQuota(long currentDiskUsage, User user, File file, CollectionImeji col)
			throws ImejiException {
		final User targetCollectionUser = col == null || user.getId().equals(col.getCreatedBy())
				? user
				: new UserService().retrieve(col.getCreatedBy(), Imeji.adminUser);
		final long needed = currentDiskUsage + file.length();
		if (needed > targetCollectionUser.getQuota()) {
			throw new QuotaExceededException("Data quota ("
					+ QuotaUtil.getQuotaHumanReadable(targetCollectionUser.getQuota(), Locale.ENGLISH)
					+ " allowed) has been exceeded (" + FileUtils.byteCountToDisplaySize(currentDiskUsage) + " used)");
		}
		return targetCollectionUser.getQuota() - needed;
	}

	/**
	 * Return the size of the used storage by the user
	 * 
	 * @param user
	 * @return
	 */
	public static long getUsedQuota(User user) {
		final Search search = SearchFactory.create(); // default: Jena
		final List<String> results = search.searchString(JenaCustomQueries.selectUserFileSize(user.getId().toString()),
				null, null, Search.SEARCH_FROM_START_INDEX, Search.GET_ALL_RESULTS).getResults();
		return Long.parseLong(results.get(0).toString());
	}

}
