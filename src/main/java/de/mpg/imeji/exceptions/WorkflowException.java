package de.mpg.imeji.exceptions;

/**
 * Exception is thrown when a non-valid workflow operation is detected (for instance: release, discard)
 *
 * @author bastiens
 *
 */
public class WorkflowException extends ImejiExceptionWithUserMessage {
  private static final long serialVersionUID = -5279563970035349584L;

  /**
   * Creates a new WorkflowException
   * 
   * @param internalMessage The internal error message that is logged
   * @param userMessageLabel An error message label for showing a meaningful user message in GUI 
   */
  public WorkflowException(String internalMessage, String userMessageLabel) {
    super(internalMessage, userMessageLabel);
  }
}
