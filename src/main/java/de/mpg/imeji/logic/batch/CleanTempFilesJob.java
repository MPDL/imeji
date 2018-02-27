package de.mpg.imeji.logic.batch;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.WildcardFileFilter;
import org.apache.log4j.Logger;

import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * f Remove all the remaining temps file created by imeji
 **/
public class CleanTempFilesJob implements Callable<Integer> {
  private static final Logger LOGGER = Logger.getLogger(CleanTempFilesJob.class);
  private static final String IMEJI_TEMP_FILE_REGEX = "imeji*";

  @Override
  public Integer call() throws Exception {
    final IOFileFilter filter = new WildcardFileFilter(IMEJI_TEMP_FILE_REGEX);
    LOGGER.info("Deleting all imeji temp file from: " + TempFileUtil.TEMP_DIR + " ...");
    final Iterator<File> iterator = FileUtils.iterateFiles(TempFileUtil.TEMP_DIR, filter, null);
    int success = 0;
    int count = 0;
    while (iterator.hasNext()) {
      final File file = iterator.next();
      try {
        count++;
        FileUtils.forceDelete(file);
        success++;
      } catch (final IOException e) {
        LOGGER.error("File " + file.getAbsolutePath() + " can not be deleted");
      }
    }
    LOGGER.info(
        "Deleting all imeji temp file done! " + success + " from " + count + " deleted tmp files.");
    return 1;
  }

}
