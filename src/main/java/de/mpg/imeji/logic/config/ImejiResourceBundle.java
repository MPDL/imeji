package de.mpg.imeji.logic.config;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

/**
 * Resource Bundle for imeji
 *
 * @author bastiens
 *
 */
public class ImejiResourceBundle {
  public static final String LABEL_BUNDLE = "labels";
  public static final String MESSAGES_BUNDLE = "messages";
  private static final Logger LOGGER = LogManager.getLogger(ImejiResourceBundle.class);

  /**
   * Returns the label according to the current user locale.
   *
   * @param placeholder A string containing the name of a label.
   * @return The label.
   */
  public String getLabel(String placeholder, Locale locale) {
    try {
      try {
        return ResourceBundle.getBundle(getSelectedLabelBundle(locale)).getString(placeholder);
      } catch (final MissingResourceException e) {
        return ResourceBundle.getBundle(getDefaultLabelBundle()).getString(placeholder);
      }
    } catch (final Exception e) {
      return placeholder;
    }
  }

 
  /** 
   *
   * Returns the message according to the current user locale.
   * 
   * @param placeholder  identifier: a string denoting the name of a message.
   * @param locale       system language of user
   * @return the message
   */
  public String getMessage(String placeholder, Locale locale) {
    
	try {
	      try {
	        return ResourceBundle.getBundle(getSelectedMessagesBundle(locale)).getString(placeholder);
	      } 
	      catch (final MissingResourceException e) {
	        return ResourceBundle.getBundle(getDefaultMessagesBundle()).getString(placeholder);
	      }
    } 
	catch (final Exception e) {
    	return placeholder;
    }
  }

  /**
   * Get a ResourceBundle for messages in the requested language (given by locale)
   * @param locale
   * @return  ResourceBundle
   */
  public ResourceBundle getMessageResourceBundle(Locale locale) throws MissingResourceException{
	  
	  ResourceBundle localeMessageResourceBundle = ResourceBundle.getBundle(getSelectedMessagesBundle(locale));
	  return localeMessageResourceBundle;

  }
  
  public void clearResourceBundleCache() {
	  ResourceBundle.clearCache();
  }
  
  
  /**
   * Get the bundle for the labels
   *
   * @return
   */
  private String getSelectedLabelBundle(Locale locale) {
    return LABEL_BUNDLE + "_" + locale.getLanguage();
  }

  /**
   * Get the default bundle for the labels
   *
   * @return
   */
  private String getDefaultLabelBundle() {
    return LABEL_BUNDLE + "_" + Locale.ENGLISH.getLanguage();
  }

  /**
   * Get the bundle for the messages
   *
   * @return
   */
  private String getSelectedMessagesBundle(Locale locale) {
    return MESSAGES_BUNDLE + "_" + locale.getLanguage();
  }

  /**
   * Get the default bundle for the messages
   *
   * @return
   */
  private String getDefaultMessagesBundle() {
    return MESSAGES_BUNDLE + "_" + Locale.ENGLISH.getLanguage();
  }
}
