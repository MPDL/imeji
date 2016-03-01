package de.mpg.imeji.logic.workflow.status;

import de.mpg.imeji.logic.vo.Properties.Status;

/**
 * Utility class for {@link Status}
 * 
 * @author bastiens
 *
 */
public class StatusUtil {

  private StatusUtil() {
    // private Constructor
  }

  /**
   * Parse a status String (for isntance http://imeji.org/terms/status#PENDING)
   * 
   * @param s
   * @return
   */
  public static Status parseStatus(String s) {
    if (Status.PENDING.getUriString().equals(s)) {
      return Status.PENDING;
    } else if (Status.RELEASED.getUriString().equals(s)) {
      return Status.RELEASED;
    } else if (Status.WITHDRAWN.getUriString().equals(s)) {
      return Status.WITHDRAWN;
    }
    return null;
  }
}
