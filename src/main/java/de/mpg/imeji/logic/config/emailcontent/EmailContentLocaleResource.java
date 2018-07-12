package de.mpg.imeji.logic.config.emailcontent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Locale;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentListXML;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentXML;
import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Access to files that provide content for email messages and email subjects in different languages 
 * @author breddin
 *
 */

public class EmailContentLocaleResource {
	
	private static final Logger LOGGER = Logger.getLogger(EmailContentLocaleResource.class);
	
	/**
	 * Contains a list of identifiers and texts that are read from file
	 */
	private EmailContentListXML emailContent;            			// internal representation of file content
	
	/**
	 * the language of the texts
	 */
	private Locale locale;                    						// language version of the file		

	/**
	 * Name of imeji property that hold the file path where xml files are stored
	 */
	private static final String imejiFilePathProperty = "imeji.tdb.path";
	
	private final File emailContentLanguageFile;
	
		
	/**
	 * Create a resource class to get access to either
	 *   - an external xml file in the file system that contains user configures texts for e-mails (subject and message) in the given language
	 *   or
	 *   - internal (part of -.war distribution) files that contain standard texts for e-mails in the given language
	 * @param locale   language 
	 */
	public EmailContentLocaleResource(Locale locale){
		
		this.locale = locale;	
		this.emailContentLanguageFile = new File(getResourceFilePath() + "emailMessages" + "_" + locale.getLanguage() + ".xml");
	}
	
	/**
	 * Get path in file system where files with email texts are stored
	 * @return file path or empty string of file path could not be retrieved from imeji properties
	 */
	public String getResourceFilePath() {
		
		String filePath = "";
		try {
			filePath = PropertyReader.getProperty(imejiFilePathProperty) + "/";
		}	
		catch(Exception e) {
			LOGGER.info("Could not read file path where files with configured texts for emails lie. "
					+ "Standard e-mail texts from distribution will be used instead.");
		}
		
		return filePath;
	}
	
	
	public EmailContentListXML getEmailMessages() {
		return this.emailContent;
	}
	
	public void setEmailContent(EmailContentListXML emailContentListXML) {
		this.emailContent = emailContentListXML; 
	}
	
	public Locale getLocale() {
		return this.locale;
	}
	
	public File getResourceXMLFile() {
		return this.emailContentLanguageFile;
	}
	
	
	public boolean resourceContentWasEditedInGUI() {		
		return this.emailContent.messagesWereEditedInGUI();
	}
	
	public void setEditedContentWasSavedToFile() {
		
	}
	
	// ---------------------------------------------------------
	//   SECTION read and write files
	// ---------------------------------------------------------
		
	/**
	 * Read customized email content (email subject and email message) from file system
	 */
	public void readEMailMessagesFromFile() throws JAXBException, IOException{
		 
		try(FileInputStream fileInput = new FileInputStream(this.emailContentLanguageFile)){
			JAXBContext jaxbContext = JAXBContext.newInstance(EmailContentListXML.class, EmailContentXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();				
			this.emailContent = (EmailContentListXML) jaxbUnmarshaller.unmarshal(fileInput);	
		}
		
		
	}
	
	/**
	 * Write email content to file if it has been edited in GUI
	 */
	public void writeToResourceAfterEditedInGUI() throws JAXBException, IOException{
		
		if(this.emailContent.messagesWereEditedInGUI()) {
			this.writeToResource();
			this.emailContent.messagesHaveBeenSaved();
		}
	}

	/**
	 * Write email content to file
	 */
	public synchronized void writeToResource() throws JAXBException, IOException{
			
		try(FileOutputStream fileout = new FileOutputStream(this.emailContentLanguageFile)){
			JAXBContext jaxbContext = JAXBContext.newInstance(EmailContentListXML.class, EmailContentXML.class);
			Marshaller jaxbMarshaller = jaxbContext.createMarshaller();		
			jaxbMarshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);		
			jaxbMarshaller.marshal(this.emailContent, fileout);	
		}
	}		
}





