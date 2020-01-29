package de.mpg.imeji.exceptions;

import java.util.Locale;

import de.mpg.imeji.logic.config.Imeji;

/**
 * Inherit this class if you want to show a user error message in GUI.
 * 
 * @author breddin
 *
 */
public abstract class ImejiExceptionWithUserMessage extends ImejiException {

  /**
   * 
   */
  private static final long serialVersionUID = 4572082308379030411L;

  /**
   * label for an error message that will be displayed by GUI to the user in the language the user
   * has chosen.
   * 
   * @see files src/main/resources/messages_*.properties
   */
  protected final String userMessageLabel;

  public ImejiExceptionWithUserMessage(String messageLabel) {
    super();
    this.userMessageLabel = messageLabel;
  }

  public ImejiExceptionWithUserMessage(String internalMessage, String userMessageLabel) {
    super(internalMessage);
    this.userMessageLabel = userMessageLabel;
  }

  public ImejiExceptionWithUserMessage(String messageLabel, Throwable e) {
    super(null, e);
    this.userMessageLabel = messageLabel;
  }

  public String getUserMessage(Locale locale) {
    if (this.userMessageLabel == null) {
      return " ";
    }
    String userMessage = Imeji.RESOURCE_BUNDLE.getMessage(this.userMessageLabel, locale);
    return userMessage;
  }


}
