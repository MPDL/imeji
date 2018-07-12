package de.mpg.imeji.logic.config.emailcontent.contentxml;

import java.util.Locale;

import javax.faces.event.ValueChangeEvent;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;



/**
 * Represents an email message that exists in different languages
 * Class holds an email message and its corresponding locale 
 * Class is used for processing (reading/writing) XML file with JAXB
 * 
 * @author breddin
 *
 */
public class EmailContentXML {
	
	
	String subject;
	String body;
	String identifier;
	String language;
	
	// back link to language list that holds this e mail message
	EmailContentListXML languageList;
	
	// XML part
	public EmailContentXML() {			
	}
	
	
	public EmailContentXML(String identifier, String subject, String body) {
		this.subject = subject;
		this.body = body;
		this.identifier = identifier;
	}
			
	@XmlElement(name = "Subject")
	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	@XmlElement(name = "Body")
	public void setBody(String body) {
		this.body = body;
	}
	
	@XmlAttribute(name = "Identifier", required = true)
	public void setIdentifier(String identifier) {
		this.identifier = identifier;
	}
	
	public String getSubject() {
		return this.subject;
	}
	
	public String getBody() {
		return this.body;
	}
	
	public String getIdentifier() {
		return this.identifier;
	}
	
	// data access and GUI part
	
	public void setLanguage(Locale locale) {
		this.language = locale.getLanguage();	
	}
	
	
	public String getLanguage() {
		return this.language;
	}
	
	public void setMessagesList(EmailContentListXML languageList) {
		this.languageList = languageList;
	}
	

	/**
	 * Remark: setter methods will get called by JSF layer always when saving (to update the model)
	 *         regardless of whether this text has been changed in GUI or not
	 *         Use listener for update of message in cache
	 * @param text
	 */
	
    public void listener(ValueChangeEvent event) {
        if(this.languageList != null) {
        	this.languageList.setMessagesWereEditedInGUI();
        }
	}
}


