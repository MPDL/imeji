package de.mpg.imeji.logic.ingest.factory;

import java.io.FileNotFoundException;

import javax.xml.bind.JAXBException;

import org.apache.log4j.Logger;
import org.xml.sax.SAXException;

import de.mpg.imeji.logic.ingest.jaxb.JaxbGenericObject;
import de.mpg.imeji.logic.vo.MetadataProfile;

public class ItemSchemaFactory {
  public Object create(MetadataProfile mdp) {

    Logger LOGGER = Logger.getLogger(ItemSchemaFactory.class);
    // Here is the schema created according to the profile
    // It must return the schema object instead of a simple Object
    // TODO
    String xmlOutputFilename = "mdp-output.xml";
    try {
      new JaxbGenericObject<MetadataProfile>(MetadataProfile.class).marshal(xmlOutputFilename, mdp);
    } catch (JAXBException e) {
      LOGGER.info("JaxB Exception", e);
    } catch (SAXException e) {
      LOGGER.info("SAX Exception", e);
    } catch (FileNotFoundException e) {
      // TODO Auto-generated catch block
      LOGGER.info("Could not find the file", e);
    }
    return null;
  }
}
