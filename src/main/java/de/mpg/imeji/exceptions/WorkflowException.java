package de.mpg.imeji.exceptions;

/**
 * Exception when ann non valid workflow operation is done (for instance: release, discard)
 * 
 * @author bastiens
 *
 */
public class WorkflowException extends ImejiException {
  private static final long serialVersionUID = -5279563970035349584L;

  public WorkflowException(String message) {
    super(message);
  }
}
