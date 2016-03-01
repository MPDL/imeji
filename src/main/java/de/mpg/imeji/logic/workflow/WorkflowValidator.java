package de.mpg.imeji.logic.workflow;

import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.beans.ConfigurationBean;
import scala.Serializable;

/**
 * Check update and delete Operation against the imeji Workflow. If not compliant, throw an error
 * 
 * @author bastiens
 *
 */
public class WorkflowValidator implements Serializable {
  private static final long serialVersionUID = -3583312940203422191L;

  /**
   * Object can be deleted if:<br/>
   * * Status is PENDING
   * 
   * @param p
   * @throws WorkflowException
   */
  public void isDeleteAllowed(Properties p) throws WorkflowException {
    if (p.getStatus() != Status.PENDING) {
      throw new WorkflowException("Workflow operation not allowed: " + p.getId()
          + " can not be deleted (current status: " + p.getStatus() + ")");
    }
  }


  /**
   * Object can be created if: <br/>
   * * if private mode, object must have Status PENDING
   * 
   * @param p
   * @throws WorkflowException
   */
  public void isCreateAllowed(Properties p) throws WorkflowException {
    if (ConfigurationBean.getPrivateModusStatic() && p.getStatus() != Status.PENDING) {
      throw new WorkflowException("Object publication is disabled!");
    }
  }

  /**
   * Can be release if: <br/>
   * * imeji is not in private Modus <br/>
   * * Status is PENDING <br/>
   * 
   * @param p
   * @return
   * @throws WorkflowException
   */
  public void isReleaseAllowed(Properties p) throws WorkflowException {
    if (ConfigurationBean.getPrivateModusStatic()) {
      throw new WorkflowException("Object publication is disabled!");
    }
    if (p.getStatus() != Status.PENDING) {
      throw new WorkflowException("Only PENDING objects can be released");
    }
  }

  /**
   * Object can be withdrawn if:<br/>
   * * Status is RELEASED
   * 
   * @param p
   * @throws WorkflowException
   */
  public void isWithdrawAllowed(Properties p) throws WorkflowException {
    if (p.getStatus() != Status.RELEASED) {
      throw new WorkflowException("Only WITHDRAWN objects can be released");
    }
  }
}
