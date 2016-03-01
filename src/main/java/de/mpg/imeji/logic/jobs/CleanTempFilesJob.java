package de.mpg.imeji.logic.jobs;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

/**
 * f Remove all the remaining temps file created by imeji
 **/
public class CleanTempFilesJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanTempFilesJob.class);
  private static String IMEJI_TEMP_FILE_REGEX = "imeji*";

  @Override
  public Integer call() throws Exception {
    IOFileFilter filter = new WildcardFileFilter(IMEJI_TEMP_FILE_REGEX);
    LOGGER.info("Deleting all imeji temp file from: " + FileUtils.getTempDirectory() + " ...");
    Iterator<File> iterator = FileUtils.iterateFiles(FileUtils.getTempDirectory(), filter, null);
    while (iterator.hasNext()) {
      File file = iterator.next();
      try {
        FileUtils.forceDelete(file);
      } catch (IOException e) {
        LOGGER.error("File " + file.getAbsolutePath() + " can not be deleted");
      }
    }
    LOGGER.info("... done!");
    return 1;
  }

}
