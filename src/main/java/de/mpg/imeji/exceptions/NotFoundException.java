package de.mpg.imeji.exceptions;

import java.util.Locale;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.util.ObjectHelper;

public class NotFoundException extends ImejiExceptionWithUserMessage {

  private static final long serialVersionUID = -6006945139992063194L;
  private String objectLabel;


  public NotFoundException(String internalMessage) {
    super(internalMessage, "error_resource_not_found");
    // minimizeStacktrace();
  }

  public NotFoundException(Object objectToFind, String internalMessage) {
    super(internalMessage, "error_not_found");
    this.objectLabel = ObjectHelper.getGUILabel(objectToFind);
  }

  public NotFoundException(Object objectToFind) {
    super("error_not_found");
    this.objectLabel = ObjectHelper.getGUILabel(objectToFind);
  }

  /**
   * Creates a user readable error message.
   * 
   * @return error message
   */
  @Override
  public String getUserMessage(Locale locale) {
    String userMessage =
        Imeji.RESOURCE_BUNDLE.getLabel(this.objectLabel, locale) + " " + Imeji.RESOURCE_BUNDLE.getMessage(this.userMessageLabel, locale);
    return userMessage;
  }
}
