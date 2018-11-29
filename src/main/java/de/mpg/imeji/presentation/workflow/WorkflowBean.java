package de.mpg.imeji.presentation.workflow;

import java.io.Serializable;

import javax.faces.bean.ManagedBean;

import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.workflow.WorkflowValidator;

/**
 * JSF Bean for usage of Workflow features
 *
 * @author bastiens
 *
 */
@ManagedBean(name = "WorkflowBean")
public class WorkflowBean implements Serializable {
  private static final long serialVersionUID = 622491364454878511L;
  private final WorkflowValidator validator = new WorkflowValidator();

  /**
   * True if the Object can be Released
   *
   * @param p
   * @return
   */
  public boolean release(Properties p) {
    try {
      validator.isReleaseAllowed(p);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * True if the Object can be released
   *
   * @param p
   * @return
   */
  public boolean withdraw(Properties p) {
    try {
      validator.isWithdrawAllowed(p);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * True if the Object can be deleted
   *
   * @param p
   * @return
   */
  public boolean delete(Properties p) {
    try {
      validator.isDeleteAllowed(p);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * True if the Object can be Released
   *
   * @param p
   * @return
   */
  public boolean createDOI(Properties p) {
    try {
      validator.isCreateDOIAllowed(p);
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

}
