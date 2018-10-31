package de.mpg.imeji.logic.config.emailcontent.contentxml;

import java.util.ArrayList;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Use for JAXB
 * 
 * @author breddin
 *
 */
@XmlRootElement(name = "EmailMessages")
public class EmailContentListXML {

	ArrayList<EmailContentXML> emailMessages; // holds a list of LocaleEmailMessages of the same language
	boolean messagesEdited; // set true after message texts have been changed in GUI

	public EmailContentListXML() {
		this.emailMessages = new ArrayList<EmailContentXML>();
	}

	@XmlElement(name = "EMailMessage")
	public void setEMailMessages(ArrayList<EmailContentXML> emailMessages) {
		this.emailMessages = emailMessages;
		this.messagesEdited = false;
	}

	public ArrayList<EmailContentXML> getEMailMessages() {
		return this.emailMessages;
	}

	public boolean messagesWereEditedInGUI() {
		return this.messagesEdited;
	}

	/**
	 * Will be called by JSF layer Indicate that a message text has been changed in
	 * GUI and changes need to be saved to file
	 * 
	 * @param event
	 */
	public void setMessagesWereEditedInGUI() {
		this.messagesEdited = true;
	}

	public void messagesHaveBeenSaved() {
		this.messagesEdited = false;
	}

}
