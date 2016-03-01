package de.mpg.imeji.logic.workflow.status;

import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Properties.Status;

/**
 * Interface to read the current {@link Status} of an Object extending {@link Properties}
 * 
 * @author bastiens
 *
 */
public interface VersionReader {

  /**
   * Read the Current {@link Status}
   * 
   * @param p
   * @return
   */
  public Status getStatus(Properties p);

  /**
   * 
   * @param p
   * @return
   */
  public int getVersion(Properties p);

}
