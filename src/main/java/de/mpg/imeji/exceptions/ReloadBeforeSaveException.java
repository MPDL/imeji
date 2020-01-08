package de.mpg.imeji.exceptions;

import java.util.Locale;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.util.ObjectHelper;

/**
 * Exception will be thrown if a process (user session) tries to update an object in store that has
 * been changed since it was last read by that same process. This can happen when another process (user
 * session) updates the object after it was read from database by our process and before it is
 * written back. Context: Multiple processes (users) access shared data resources (i.e. collection, item).
 * 
 * The latest version of the object in store can be retrieved from the Exception.
 * 
 * A user readable error message (in different languages) is available.
 * 
 * @author breddin
 *
 */
public class ReloadBeforeSaveException extends ImejiExceptionWithUserMessage {

  private static final long serialVersionUID = -2208211062675780202L;
  
  /**
   * Current (latest) version of the object in store
   */
  private Object latestDataObject;
  
  /**
   * User readable name of the object that needs to be reloaded
   * for GUI error message
   */
  private String objectLabel;

  
  public ReloadBeforeSaveException(Object latestDataObject) {

	super("reload_before_save");
    this.objectLabel = ObjectHelper.getGUILabel(latestDataObject);
    this.latestDataObject = latestDataObject;
  }

  public Object getLatestDataObject() {
    return this.latestDataObject;
  }

  
  /**
   * Creates a user readable error message.
   * @return error message
   */
  @Override
  public String getUserMessage(Locale locale) {
		String userMessage = 
				Imeji.RESOURCE_BUNDLE.getLabel(this.objectLabel, locale) + " " +
				Imeji.RESOURCE_BUNDLE.getMessage(this.userMessageLabel, locale);
	  return userMessage;
	}
}
