package de.mpg.imeji.logic.workflow;

import java.util.Calendar;

import de.mpg.imeji.exceptions.NotSupportedMethodException;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.model.Properties;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.util.DateHelper;

/**
 * Prepare Objects for Workflow operations.<br/>
 * NOTE: Objects are not written in the database. This must be done by the controllers
 *
 * @author bastiens
 *
 */
public class WorkflowManager {

  private final WorkflowValidator workflowValidator = new WorkflowValidator();

  /**
   * Prepare the creation of an object: Set all Workflow properties
   *
   * @param p
   * @param user
   * @throws WorkflowException
   */
  public void prepareCreate(Properties p, User user) throws WorkflowException {
    workflowValidator.isCreateAllowed(p);
    J2JHelper.setId(p, IdentifierUtil.newURI(p.getClass()));
    final Calendar now = DateHelper.getCurrentDate();
    if (user != null) {
      p.setCreatedBy(user.getId());
      p.setModifiedBy(user.getId());
    }
    p.setCreated(now);
    if (p.getStatus() == null) {
      p.setStatus(Status.PENDING);
    }
  }

  /**
   * Prepare the Update of an object
   *
   * @param p
   * @param user
   */
  public void prepareUpdate(Properties p, User user) {
    p.setModifiedBy(user.getId());
  }

  /**
   * Prepare the release of an object
   *
   * @param p
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   */
  public void prepareRelease(Properties p) throws NotSupportedMethodException, WorkflowException {
    workflowValidator.isReleaseAllowed(p);
    p.setStatus(Status.RELEASED);
    p.setVersionDate(DateHelper.getCurrentDate());
  }

  /**
   * Prepare the withdraw of an object
   *
   * @param p
   * @throws WorkflowException
   * @throws NotSupportedMethodException
   */
  public void prepareWithdraw(Properties p) throws WorkflowException, NotSupportedMethodException {
    workflowValidator.isWithdrawAllowed(p);
    if (p.getDiscardComment() == null || "".equals(p.getDiscardComment())) {
      throw new WorkflowException("Discard error: A Discard comment is needed", "error_withdraw_comment");
    }
    p.setStatus(Status.WITHDRAWN);
  }
}
