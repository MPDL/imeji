package de.mpg.imeji.logic.config.emailcontent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.ImejiConfiguration;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentListXML;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentXML;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Configure content of e-mails (email subject and email message) that can be sent to users and
 * system administrators
 * 
 * Administrators change texts (subject and message) of e-mails via GUI Admin > Configuration >
 * Email messages
 * 
 * @author breddin
 *
 */

public class ImejiEmailContentConfiguration {

  private static final Logger LOGGER = LogManager.getLogger(ImejiEmailContentConfiguration.class);

  // naming conventions
  private static String MESSAGE_GUI_LABEL_ID_PREFIX = "config_messages_";
  private static String MESSAGE_RESOURCE_BUNDLE_SUBJECT_POSTFIX = "_subject";
  private static String MESSAGE_RESOURCE_BUNDLE_BODY_POSTFIX = "_body";

  /**
   * list of resource classes that provide access to files files store texts together with with
   * labels/identifiers for the texts
   */
  private LinkedList<EmailContentLocaleResource> contentResources;

  /**
   * Local cache for email content Texts are read from file to cache on start up of server Changes
   * of texts in cache are immediately stored (back) to file HashMap provides quick access to an
   * email content (in a certain language) given the language and an identifier (String) of the
   * content
   */
  private HashMap<String, List<EmailContentXML>> contentCache;

  /**
   * Constructor of ImejiEmailMessageConfiguration Needs a link to ImejiConfiguration object.
   * ImejiConfiguration object holds the system/Imeji instance languages. These are needed to access
   * text files
   * 
   * @param imejiConfiguration
   */
  public ImejiEmailContentConfiguration(ImejiConfiguration imejiConfiguration) {

    init(imejiConfiguration);

  }

  // ------------------------------------------------------------------------------------------
  // SECTION Initialize, reset and reload email content to cache
  // -----------------------------------------------------------------------------------------

  /**
   * Initializes the local cache of email content by - reading email content from XML files in file
   * system - storing read content in a hash map for fast access
   * 
   * @param imejiConfiguration
   */
  public void init(ImejiConfiguration imejiConfiguration) {

    // (1) initiate list for file/resource access classes
    this.contentResources = new LinkedList<EmailContentLocaleResource>();

    // (2) read content from xml files to local resource classes
    readMessagesInAllLanguagesFromFile(imejiConfiguration);

    // (3) re-organize content from local resource classes into a hash map (for fast
    // access)
    constructHashMap();
  }

  /**
   * Call this function after admin role changed the system languages to update email content in
   * local cache file system
   * 
   * @param imejiConfiguration current/latest system configuration, contains system languages
   */
  private void reloadCacheAfterSystemLanguagesChanged(ImejiConfiguration imejiConfiguration) {

    // prepare
    List<String> systemLanguageCodes = imejiConfiguration.getLanguagesAsList();
    LinkedList<String> newLanguagesCodes = getNewLanguagesForCache(systemLanguageCodes);
    // if a new language has been added to system languages:
    for (String newLanguageCode : newLanguagesCodes) {
      ImejiExternalEmailContent.addNewLanguage(newLanguageCode);
    }
    // re-init
    this.init(imejiConfiguration);
  }

  /**
   * Compares the list of system languages to the languages that are stored in cache
   * 
   * @param systemLanguageCodes
   * @return list of languages that are set as system languages but not in cache yet
   */
  private LinkedList<String> getNewLanguagesForCache(List<String> systemLanguageCodes) {

    LinkedList<String> addTheseLanguagesToCache = new LinkedList<String>();

    for (String systemLanguageCode : systemLanguageCodes) {
      // check: does the language exist in cache? if not, add to list of new languages
      boolean languageAlreadyInCache = false;
      for (EmailContentLocaleResource mailContentResource : this.contentResources) {
        if (systemLanguageCode.equalsIgnoreCase(mailContentResource.getLocale().getLanguage())) {
          languageAlreadyInCache = true;
        }
      }
      if (!languageAlreadyInCache) {
        addTheseLanguagesToCache.add(systemLanguageCode);
      }
    }
    return addTheseLanguagesToCache;
  }

  // ------------------------------------------------------------------------------
  // SECTION public interface for getting email content
  // -----------------------------------------------------------------------------

  /**
   * Get email message body from local cache
   * 
   * @param identifier
   * @param locale
   * @return
   */

  public String getMessageBody(String identifier, Locale locale) {

    if (contentCache.containsKey(identifier)) {
      List<EmailContentXML> messageInDifferentLanguages = contentCache.get(identifier);
      for (EmailContentXML message : messageInDifferentLanguages) {
        if (message.getLanguage().equals(locale.getLanguage())) {
          return message.getBody();
        }
      }
    }
    // try to read the content (message) from the internal
    // message_[en,de,es,jp].properties files
    return getMessageFromResourceBundleBody(identifier, locale);

  }

  /**
   * Get email message subject from local cache
   * 
   * @param identifier
   * @param locale
   * @return
   */
  public String getMessageSubject(String identifier, Locale locale) {

    // (a) look for the message text in the message cache
    if (contentCache.containsKey(identifier)) {
      List<EmailContentXML> messageInDifferentLanguages = contentCache.get(identifier);
      for (EmailContentXML message : messageInDifferentLanguages) {
        if (message.getLanguage().equals(locale.getLanguage())) {
          return message.getSubject();
        }
      }

      // Error 1: text in requested language was not in cache
    }
    // Error 2: identifier was not in cache
    // -> try to read the message text from the internal
    // message_[en,de,es,jp].properties files
    return getMessageFromResourceBundleSubject(identifier, locale);

  }

  /**
   * Read text for message body from resource bundle (messages_[en,de,ja,es].properties) Returns
   * text (String) for given identifier If no text is found method will return an empty String
   * 
   * @param identifier
   * @param locale
   * @return
   */
  private String getMessageFromResourceBundleBody(String identifier, Locale locale) {

    // the message identifier used in .xml is a "word" stem,
    // in order to get the corresponding message from internal
    // message_[en,de,es,jp].properties files
    // in some cases a "_body" postfix must be added

    // (1) try without postfix
    String messageBody = Imeji.RESOURCE_BUNDLE.getMessage(identifier, locale);
    // identifier not found
    if (messageBody.equalsIgnoreCase(identifier)) {
      // (2) try with postfix
      String identifierWithPostfix = identifier + MESSAGE_RESOURCE_BUNDLE_BODY_POSTFIX;
      messageBody = Imeji.RESOURCE_BUNDLE.getMessage(identifierWithPostfix, locale);
    }
    return messageBody;
  }

  /**
   * Read text for message subject from resource bundle (messages_[en,de,ja,es].properties) Returns
   * text (String) for given identifier If no text is found method will return an empty String
   * 
   * @param identifier
   * @param locale
   * @return
   */
  private String getMessageFromResourceBundleSubject(String identifier, Locale locale) {

    // the message identifier used in .xml is a "word" stem,
    // in order to get the corresponding message from internal
    // message_[en,de,es,ja].properties files
    // a "_subject" postfix must be added

    String resourceBundleIdentifier = identifier + MESSAGE_RESOURCE_BUNDLE_SUBJECT_POSTFIX;
    String messageSubject = Imeji.RESOURCE_BUNDLE.getMessage(resourceBundleIdentifier, locale);
    return messageSubject;
  }

  // --------------------------------------------------------------------------------------------------
  // SECTION functions for GUI configuration (edit, create, delete) of email
  // message texts
  // --------------------------------------------------------------------------------------------------

  public List<List<EmailContentXML>> getAllEmailMessagesInAllLanguages() {

    List<List<EmailContentXML>> emailsInAllLanguages = new ArrayList<List<EmailContentXML>>();
    Set<String> allIdentifiers = this.contentCache.keySet();
    for (String identifier : allIdentifiers) {
      List<EmailContentXML> list = this.contentCache.get(identifier);
      emailsInAllLanguages.add(list);
    }
    return emailsInAllLanguages;
  }

  // currently not used
  public List<EmailContentXML> getAllEmailMessagesInAllLanguagesAsList() {

    // Flatten the hash map and build a simple list that contains all entries of the
    // hash map
    // list will be used to present data (as LocaleText objects) in GUI
    List<EmailContentXML> allMessages = new ArrayList<EmailContentXML>();
    Set<String> allIdentifiers = this.contentCache.keySet();
    for (String identifier : allIdentifiers) {
      List<EmailContentXML> list = this.contentCache.get(identifier);
      for (EmailContentXML localeMessage : list) {
        allMessages.add(localeMessage);
      }
    }
    return allMessages;
  }

  /**
   * Get the GUI label for an email message identifier in order to show in GUI
   * 
   * @param identifier identifier for an email message text, i.e. email_password_reset, see
   *        ..\data\imeji\tdb\emailMessages_en.xml
   * @param locale current system language
   * @return the label
   */
  public String getGUILabelForEMailMessageIdentifier(String identifier, Locale locale) {

    // convention: labels for message identifiers start with "config_messages_" +
    // identifier
    // example: identifier: email_zip_images_downloaded , label-id =
    // config_messages_email_zip_images_downloaded
    String labelIdentifier = MESSAGE_GUI_LABEL_ID_PREFIX + identifier;
    return Imeji.RESOURCE_BUNDLE.getLabel(labelIdentifier, locale);
  }

  // --------------------------------------------------------------------------
  // SECTION read email content from files into local cache
  // --------------------------------------------------------------------------

  /**
   * Data from local resource classes gets restructured and organized in a HashMap that maps
   * identifier1 -> (text1, languageA), (text1, languageB), (text1, languageC)
   * 
   */
  private void constructHashMap() {

    // re-organize the information in order to get this data structure:
    // identifier -> (Text, Language), (Text, Language), (Text, Language)
    this.contentCache = new HashMap<String, List<EmailContentXML>>();

    for (EmailContentLocaleResource messageResource : this.contentResources) {
      // get the Properties of each file
      // get the key set of the properties
      // use the key set to build a hashmap that stores
      // identifier -> (Text, Language), (Text, Language), (Text, Language)
      // find for each key the corresponding text in all languages
      EmailContentListXML messagesXML = messageResource.getEmailMessages();
      ArrayList<EmailContentXML> messagesInOneLanguage = messagesXML.getEMailMessages();
      for (Object messageObject : messagesInOneLanguage) {
        if (messageObject instanceof EmailContentXML) {
          EmailContentXML localeMessage = (EmailContentXML) messageObject;
          String identifier = localeMessage.getIdentifier();
          localeMessage.setLanguage(messageResource.getLocale());
          localeMessage.setMessagesList(messagesXML);

          // (a) identifier already exists
          if (contentCache.containsKey(identifier)) {
            List<EmailContentXML> list = contentCache.get(identifier);
            list.add(localeMessage);
            contentCache.put(identifier, list);
          }
          // (b) identifier doesn't exist yet
          else {
            List<EmailContentXML> list = new LinkedList<EmailContentXML>();
            list.add(localeMessage);
            contentCache.put(identifier, list);
          }
        }
      }
    }
  }

  private void readMessagesInAllLanguagesFromFile(ImejiConfiguration imejiConfiguration) {

    if (imejiConfiguration != null) {
      LinkedList<String> systemLanguages = imejiConfiguration.getLanguagesAsList();
      // if there are no system languages specified, get the default language to use
      if (systemLanguages.size() == 0) {
        systemLanguages.add(Locale.ENGLISH.getLanguage());
      }
      for (String language : systemLanguages) {
        Locale locale = new Locale(language);
        EmailContentLocaleResource emailMessageResource = new EmailContentLocaleResource(locale);
        try {
          emailMessageResource.readEMailMessagesFromFile();
          this.contentResources.add(emailMessageResource);
        } catch (IOException ioe) {
          LOGGER.info("Could not read " + emailMessageResource.getResourceXMLFile().getAbsolutePath()
              + ". Feature configure email content will not be available in language "
              + emailMessageResource.getLocale().getDisplayLanguage());
        } catch (JAXBException jaxbException) {
          LOGGER.info("Problem with JAX binding in " + emailMessageResource.getResourceXMLFile().getAbsolutePath()
              + ". Cannot access this file with configured texts for emails. Standard e-mail texts from distribution will be used instead.");
          LOGGER.info(jaxbException.toString());
        }
      }
    } else {
      LOGGER.info("Could not get system languages from ImejiConfiguration instance. "
          + "Cannot access files with configured texts for emails. Standard e-mail texts from distribution will be used instead.");
    }
  }

  // --------------------------------------------------------------------------
  // SECTION save cache to file
  // --------------------------------------------------------------------------

  /**
   * Save current state of cache to file.
   */
  public void save() {

    if (this.contentResources != null) {
      for (EmailContentLocaleResource messageResource : this.contentResources) {
        try {
          messageResource.writeToResource();;
        } catch (IOException ioe) {
          LOGGER.info("Could not write to " + messageResource.getResourceXMLFile().getAbsolutePath());
          LOGGER.info(ioe.getMessage());
        } catch (JAXBException jaxbException) {
          LOGGER.info("Problem with JAX binding in " + messageResource.getResourceXMLFile().getAbsolutePath()
              + ". Changes in email texts will not be saved to file.");
          LOGGER.info(jaxbException.toString());
        }
      }
    }
  }

  /**
   * Function checks - if email content was edited and should be saved to file - if system languages
   * have changed and cache needs updating
   */
  public void saveChangesAndUpdate(ImejiConfiguration imejiConfiguration) {
    // save content if it has been edited in GUI
    this.saveChanges();
    // in case we have knowledge of XML files, we have email content cached and now
    // system languages have changed
    if (this.contentResources.size() > 0 && !this.contentCache.isEmpty()) {
      this.reloadCacheAfterSystemLanguagesChanged(imejiConfiguration);
    }
  }

  /**
   * Save changes to file. Call after editing email content in Admin > Configuration > Messages view
   * is done.
   */
  private void saveChanges() {

    if (this.contentResources != null) {
      for (EmailContentLocaleResource messageResource : this.contentResources) {
        try {
          messageResource.writeToResourceAfterEditedInGUI();
        } catch (IOException ioe) {
          LOGGER.info("Could not write to " + messageResource.getResourceXMLFile().getAbsolutePath());
          LOGGER.info(ioe.getMessage());
        } catch (JAXBException jaxbException) {
          LOGGER.info("Problem with JAX binding in " + messageResource.getResourceXMLFile().getAbsolutePath()
              + ". Changes in email texts will not be saved to file.");
          LOGGER.info(jaxbException.toString());
        }
      }
    }
  }

}
