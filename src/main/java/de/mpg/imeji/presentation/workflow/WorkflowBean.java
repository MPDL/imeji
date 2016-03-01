package de.mpg.imeji.presentation.workflow;

import javax.faces.bean.ManagedBean;

import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.workflow.WorkflowValidator;
import scala.Serializable;

/**
 * JSF Bean for usage of Workflow features
 * 
 * @author bastiens
 *
 */
@ManagedBean(name = "WorkflowBean")
public class WorkflowBean implements Serializable {
  private static final long serialVersionUID = 622491364454878511L;
  private WorkflowValidator validator = new WorkflowValidator();

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
    } catch (WorkflowException e) {
      return false;
    }
  }

  /**
   * True if the Object ca be released
   * 
   * @param p
   * @return
   */
  public boolean withdraw(Properties p) {
    try {
      validator.isWithdrawAllowed(p);
      return true;
    } catch (WorkflowException e) {
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
    } catch (WorkflowException e) {
      return false;
    }
  }
}
