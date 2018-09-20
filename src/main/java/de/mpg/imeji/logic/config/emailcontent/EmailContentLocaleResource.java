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

import org.apache.logging.log4j.Logger; 
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentListXML;
import de.mpg.imeji.logic.config.emailcontent.contentxml.EmailContentXML;
import de.mpg.imeji.logic.config.util.PropertyReader;

/**
 * Access to files that provide content for email messages and email subjects in different languages 
 * @author breddin
 *
 */

public class EmailContentLocaleResource {
	
	private static final Logger LOGGER = LogManager.getLogger(EmailContentLocaleResource.class);
	
	
	/**
	 * Associated file that stores email content
	 */
	private final File emailContentLanguageFile;
	
	/**
	 * Language of email content and language of the associated file
	 */
	private Locale locale; 
	
	/**
	 * Name of an imeji property that holds the file path to the associated file
	 */
	private static final String imejiFilePathProperty = "imeji.tdb.path";
	
	/**
	 * Internal representation of content of the associated file
	 * Data structure contains a list of identifiers and mapped email content
	 */
	private EmailContentListXML emailContent;  
		
		
	/**
	 * Create a resource class to access a XML file in the file system 
	 * that contains user configures content for e-mails (subject and message) in the given language
	 * 
	 * @param locale   language 
	 */
	public EmailContentLocaleResource(Locale locale){
		
		this.locale = locale;	
		this.emailContentLanguageFile = new File(getResourceFilePath() + "emailMessages" + "_" + locale.getLanguage() + ".xml");
	}
	
	
	/**
	 * Read the path in file system where the associated XML file is stored
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
	
	public boolean resourceXMLFileExists() {
		return this.emailContentLanguageFile.exists();
	}
	
	public boolean resourceContentWasEditedInGUI() {		
		return this.emailContent.messagesWereEditedInGUI();
	}
	
	// ---------------------------------------------------------
	//   SECTION read and write files
	// ---------------------------------------------------------
		
	/**
	 * Read customized email content (email subject and email message) from an 
	 * associated file in the file system.
	 * Cache this information in a map-like data structure 
	 */
	public void readEMailMessagesFromFile() throws JAXBException, IOException{
		 
		try(FileInputStream fileInput = new FileInputStream(this.emailContentLanguageFile)){
			JAXBContext jaxbContext = JAXBContext.newInstance(EmailContentListXML.class, EmailContentXML.class);
			Unmarshaller jaxbUnmarshaller = jaxbContext.createUnmarshaller();				
			this.emailContent = (EmailContentListXML) jaxbUnmarshaller.unmarshal(fileInput);	
		}
		
		
	}
	
	/**
	 * Write email content to associated file 
	 * only if the content has been changed/edited in GUI before
	 */
	public void writeToResourceAfterEditedInGUI() throws JAXBException, IOException{
		
		if(this.emailContent.messagesWereEditedInGUI()) {
			this.writeToResource();
			this.emailContent.messagesHaveBeenSaved();
		}
	}

	/**
	 * Write email content to associated file
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





