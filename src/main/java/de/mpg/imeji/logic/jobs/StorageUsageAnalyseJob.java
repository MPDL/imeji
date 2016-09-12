package de.mpg.imeji.logic.jobs;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Properties;
import java.util.concurrent.Callable;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;

/**
 * Analyze the File system of the {@link Storage} and save the results in a property file
 *
 * @author saquet
 *
 */
public class StorageUsageAnalyseJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(StorageUsageAnalyseJob.class);

  public enum StorageUsage {
    NUMBER_OF_FILES, STORAGE_USED, FREE_SPACE, LAST_UPDATE_DATE;
  }

  private static File STORAGE_USAGE_STATS_FILE_NAME;
  private Properties storageProperties = new Properties();

  /**
   * Constructor
   *
   * @throws IOException
   * @throws URISyntaxException
   */
  public StorageUsageAnalyseJob() throws IOException, URISyntaxException {
    STORAGE_USAGE_STATS_FILE_NAME =
        new File(PropertyReader.getProperty("imeji.tdb.path") + "/storageStats.xml");
    if (!STORAGE_USAGE_STATS_FILE_NAME.exists()) {
      STORAGE_USAGE_STATS_FILE_NAME.createNewFile();
    }
    try {
      storageProperties.loadFromXML(new FileInputStream(STORAGE_USAGE_STATS_FILE_NAME));
    } catch (Exception e) {
      LOGGER.info("Can not read " + STORAGE_USAGE_STATS_FILE_NAME + " (Probably empty)");
    }
  }

  @Override
  public Integer call() throws Exception {
    LOGGER.info("Analysing the storage...");
    StorageController sc = new StorageController();
    storageProperties.setProperty(StorageUsage.NUMBER_OF_FILES.name(),
        Long.toString(sc.getAdministrator().getNumberOfFiles()));
    storageProperties.setProperty(StorageUsage.STORAGE_USED.name(),
        Long.toString(sc.getAdministrator().getSizeOfFiles()));
    storageProperties.setProperty(StorageUsage.FREE_SPACE.name(),
        Long.toString(sc.getAdministrator().getFreeSpace()));
    storageProperties.setProperty(StorageUsage.LAST_UPDATE_DATE.name(), getCurrentDate());
    storageProperties.storeToXML(new FileOutputStream(STORAGE_USAGE_STATS_FILE_NAME), null,
        "UTF-8");
    LOGGER.info("...done");
    return 1;
  }

  /**
   * REturn the Current Date as String
   *
   * @return
   */
  private String getCurrentDate() {
    DateFormat dateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
    Calendar cal = Calendar.getInstance();
    return dateFormat.format(cal.getTime());
  }

  public int getNumberOfFiles() {
    if (storageProperties.get(StorageUsage.NUMBER_OF_FILES.name()) == null) {
      return 0;
    }
    return Integer.parseInt((String) storageProperties.get(StorageUsage.NUMBER_OF_FILES.name()));
  }

  public long getStorageUsed() {
    if (storageProperties.get(StorageUsage.STORAGE_USED.name()) == null) {
      return 0;
    }
    return Long.parseLong((String) storageProperties.get(StorageUsage.STORAGE_USED.name()));
  }

  public long getFreeSpace() {
    if (storageProperties.get(StorageUsage.FREE_SPACE.name()) == null) {
      return 0;
    }
    return Long.parseLong((String) storageProperties.get(StorageUsage.FREE_SPACE.name()));
  }

  public String getLastUpdate() {
    if (storageProperties.get(StorageUsage.LAST_UPDATE_DATE.name()) == null) {
      return "-";
    }
    return (String) storageProperties.get(StorageUsage.LAST_UPDATE_DATE.name());
  }
}
