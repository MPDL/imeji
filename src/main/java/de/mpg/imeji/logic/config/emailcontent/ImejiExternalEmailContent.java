package de.mpg.imeji.logic.config.emailcontent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import javax.xml.bind.JAXBException;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentListXML;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentXML;

/**
 * Class takes care of copying content of e-mails (messages and subjects) from
 * internal .war resources files (messages_[en,de,es,ja].properties) to external
 * xml files. These are stored in the server's file system. After the texts have
 * been copied and stored outside the project the Imeji administrator can edit
 * them in the Admin > Config > EMail Messages view Copying/restoring of email
 * content is done via Admin > Tools > Copy email content
 * 
 * @author breddin
 *
 */
public class ImejiExternalEmailContent {

	private static final Logger LOGGER = LogManager.getLogger(ImejiExternalEmailContent.class);

	/*
	 * messages_[en,de,es,ja].properties contains body/subject for 12 different
	 * mails Some subjects and bodys that exist in messages_[en,de,es,ja].properties
	 * are not used in Imeji These are maked with "(not used)" and will not be
	 * copied from messages_[en,de,es,ja].properties
	 * 
	 * Mail content:
	 * 
	 * (currently used and copied) email_registration_request_subject,
	 * email_registration_request_body email_account_activation_body,
	 * email_account_activation_subject email_password_reset_body,
	 * email_password_reset_subject email_new_password, email_new_password_subject
	 * email_invitation_body, email_invitation_subject email_shared_collection,
	 * email_shared_collection_subject email_subscribtion_body,
	 * email_subscribtion_subject
	 * 
	 * (currently not in use and not copied) email_item_downloaded_body,
	 * email_item_downloaded_subject email_new_user, email_new_user_subject
	 * email_zip_images_downloaded_body, email_zip_images_downloaded_subject
	 * email_unshared_object email_shared_item
	 */

	/*
	 * identifier of mails that will be copied from internal
	 * messages_[en,de,es,ja].properties
	 */
	private static String[] emailContentIdentifiersBody = {"email_account_activation_body", "email_invitation_body",
			"email_new_password", "email_password_reset_body", "email_registration_request_body",
			"email_shared_collection", "email_subscribtion_body"};

	private static String[] emailContentIdentifiersSubject = {"email_account_activation_subject",
			"email_invitation_subject", "email_new_password_subject", "email_password_reset_subject",
			"email_registration_request_subject", "email_shared_collection_subject", "email_subscribtion_subject"};

	// identifier of mail content in newly created xml files
	// remark: in messages_[en,de,es,ja].properties content identifiers are marked
	// with "subject" and "body"
	// in xml there is a representation of a complete email (subject and body)
	// as an identifier for the complete email the word-stem is used and postfixes
	// "subject" and "body" are omitted
	// example:
	// "email_account_activation_body" (identifier for message of
	// email_account_activation)
	// "email_account_activation_subject" (identifier for subject of
	// email_account_activation)
	// -> xml identifier for whole email: "email_account_activation"

	private static String[] emailContentIdentifiersXML = {"email_account_activation", "email_invitation",
			"email_new_password", "email_password_reset", "email_registration_request", "email_shared_collection",
			"email_subscribtion"};

	/**
	 * Function should only be called from GUI Admin > Tools > Edit email content
	 * button Function takes the current system languages and copies all email
	 * content from internal files (.properties) to xml files in the file system
	 */
	public static void copyEmailContentToExternalXMLFiles() {

		if (Imeji.RESOURCE_BUNDLE != null && Imeji.CONFIG != null) {

			LinkedList<String> systemLanguages = Imeji.CONFIG.getLanguagesAsList();
			for (String languageCode : systemLanguages) {
				copyEmailContentToExternalXMLFile(languageCode);
			}

			// notify ImejiEmailContentConfiguration that file content has changed
			// and needs to be re-loaded
			if (Imeji.EMAIL_CONFIG != null) {
				Imeji.EMAIL_CONFIG.init(Imeji.CONFIG);
			} else {
				LOGGER.info("Unexpected: Imeji.EMAIL_CONFIG is null");
			}
		} else {
			LOGGER.info("Could not get system languages from ImejiConfiguration instance. "
					+ "Cannot access files with configured texts for emails. Standard e-mail texts from distribution will be used instead.");
		}
	}

	/**
	 * Call this function to add a new language to the external email content files.
	 * 
	 * In case no XML file with email content in this language exists yet (and can
	 * be read from file system) a new XML file is created by copying email content
	 * from internal messages_[en,de,es,ja].properties file
	 * 
	 * @param languageCode
	 */
	public static void addNewLanguage(String languageCode) {

		// Look in file system if for the given language an XML file for email content
		// exists
		EmailContentLocaleResource languageResource = new EmailContentLocaleResource(new Locale(languageCode));
		if (!languageResource.resourceXMLFileExists()) {
			// Copy email content from .properties file
			copyEmailContentToExternalXMLFile(languageCode);
		}
	}

	/**
	 * Reads email content (subject and message) in the given language from an
	 * internal messages_[en,es,de,ja].properties file Converts read email content
	 * to XML structure Writes XML structured email content to an XML file in the
	 * file system
	 * 
	 * @param languageCode
	 *            should be 'en','es','de','ja'
	 */
	private static void copyEmailContentToExternalXMLFile(String languageCode) {

		ResourceBundle languageBundle = null;

		// (1) read messages_[en,de,es,ja].properties file via Imeji.RESOURCE_BUNDLE
		Locale languageLocale = new Locale(languageCode);
		try {
			languageBundle = Imeji.RESOURCE_BUNDLE.getMessageResourceBundle(languageLocale);
		} catch (MissingResourceException missingResourceException) {
			LOGGER.info("Couldn't find a ResourceBundle for language code " + languageCode
					+ ". No texts, labels, messages for language " + languageCode + " are available for Imeji");
		}

		if (languageBundle != null) {

			// (2) organize content for XML representation
			ArrayList<EmailContentXML> xmlContentList = new ArrayList<EmailContentXML>();
			for (int j = 0; j < emailContentIdentifiersBody.length; j++) {
				String messageBody = null;
				String messageSubject = null;
				try {
					messageBody = languageBundle.getString(emailContentIdentifiersBody[j]);
					messageSubject = languageBundle.getString(emailContentIdentifiersSubject[j]);
				} catch (MissingResourceException missingResource) {
					// identifier is missing in a language
				}

				String xmlMessageIdentifier = emailContentIdentifiersXML[j];

				// only create xml element if there is at least some content (body or head)
				if (messageBody != null || messageSubject != null) {
					EmailContentXML xmlMessage = new EmailContentXML(xmlMessageIdentifier, messageSubject, messageBody);
					xmlContentList.add(xmlMessage);
				}
			}

			// (3) write to XML file
			EmailContentListXML emailContentInOneLangaugeXML = new EmailContentListXML();
			emailContentInOneLangaugeXML.setEMailMessages(xmlContentList);

			EmailContentLocaleResource xmlWriter = new EmailContentLocaleResource(languageLocale);
			xmlWriter.setEmailContent(emailContentInOneLangaugeXML);
			try {
				xmlWriter.writeToResource();
			} catch (JAXBException exception) {
				LOGGER.info("Could not write email content from project to xml file " + xmlWriter.getResourceFilePath()
						+ ".  Standard e-mail content from distribution will be used instead.");
			} catch (IOException e) {
				LOGGER.info("Could not write to " + xmlWriter.getResourceFilePath()
						+ ".  Standard e-mail content from distribution will be used instead.");
			}
		}
	}

}
