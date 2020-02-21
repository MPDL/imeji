package de.mpg.imeji.logic.doi.util;

import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.doi.models.DOICollection;
import de.mpg.imeji.logic.doi.models.DOICreator;
import de.mpg.imeji.logic.doi.models.DOIIdentifier;
import de.mpg.imeji.logic.doi.models.DOIResourceType;
import de.mpg.imeji.logic.doi.models.DOITitle;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Person;

/**
 * Utility Class for DOI Service
 *
 * @author bastiens
 *
 */
public class DOIUtil {
  private static final Logger LOGGER = LogManager.getLogger(DOIUtil.class);
  private static final Client client = ClientBuilder.newClient();

  private DOIUtil() {
    // private controller
  }

  public static DOICollection transformToDOICollection(CollectionImeji col) {
    DOICollection dcol = new DOICollection();
    DOITitle title = new DOITitle(col.getTitle());
    List<DOICreator> creators = new ArrayList<>();
    for (Person author : col.getPersons()) {
      creators.add(new DOICreator(author.getCompleteName()));
    }
    dcol.setIdentifier(new DOIIdentifier());
    dcol.getTitles().add(title);
    dcol.getCreators().setCreator(creators);
    dcol.setPublicationYear(String.valueOf(col.getCreated().get(Calendar.YEAR)));
    dcol.setResourceType(new DOIResourceType());
    return dcol;
  }

  public static String convertToXML(DOICollection dcol) throws ImejiException {
    final StringWriter sw = new StringWriter();

    try {
      final JAXBContext context = JAXBContext.newInstance(DOICollection.class);
      Marshaller m;
      m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
          "http://datacite.org/schema/kernel-4 http://schema.datacite.org/meta/kernel-4.3/metadata.xsd");
      m.marshal(dcol, sw);
    } catch (final JAXBException e) {
      throw new ImejiException("Error occured, when contacting DOxI.");
    }

    final String xml = sw.toString().trim();
    return xml;
  }

  public static String convertToXML(CollectionImeji collection) throws ImejiException {
    DOICollection doiCollection = DOIUtil.transformToDOICollection(collection);
    final String xml = DOIUtil.convertToXML(doiCollection);

    return xml;
  }

  /**
   * Sends a HTTP PUT method request to the DOI service.
   * 
   * A new DOI for the given URL is created by the DOI service. If the URL is null, a draft DOI is
   * created.
   * 
   * @param doiServiceUrl
   * @param doiUser
   * @param doiPassword
   * @param url
   * @param xml
   * @return the request response.
   * @throws ImejiException if an error status code is returned (or service url validation fails).
   */
  public static String makeDOIPutRequest(String doiServiceUrl, String doiUser, String doiPassword, String url, String xml)
      throws ImejiException {
    // Trim to avoid errors due to unwanted spaces
    doiServiceUrl = doiServiceUrl.trim();
    validateURL(doiServiceUrl);

    final Response response = client.target(doiServiceUrl).queryParam("url", url)
        .register(HttpAuthenticationFeature.basic(doiUser, doiPassword)).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.TEXT_PLAIN).put(Entity.entity(xml, "text/xml"));

    final int statusCode = response.getStatus();
    final String responseEntity = response.readEntity(String.class);

    if (statusCode == HttpStatus.SC_CREATED) {
      LOGGER.info("DOI {} sucessfully created by DOXI.", responseEntity);
    } else if (statusCode == HttpStatus.SC_ACCEPTED) {
      LOGGER.info("Draft DOI {} sucessfully created by DOXI.", responseEntity);
    } else {
      LOGGER.error("Error occured, when contacting DOXI. StatusCode = {} - Parameters: URL = {}, XML = \n{}", statusCode, url, xml);
      throw new ImejiException("Error creating DOI. StatusCode=" + statusCode + " - " + HttpStatus.getStatusText(statusCode) + " - "
          + responseEntity + ". Please contact your admin!");
    }

    return responseEntity;
  }

  /**
   * Sends a HTTP POST method request to the DOI service.
   * 
   * The URL and metadata (body) for the given DOI are updated by the DOI service.
   * 
   * @param doiServiceUrl
   * @param doiUser
   * @param doiPassword
   * @param url
   * @param doi
   * @param body
   * @return the request response.
   * @throws ImejiException if an error status code is returned (or service url validation fails).
   */
  public static String makeDOIPostRequest(String doiServiceUrl, String doiUser, String doiPassword, String url, String doi, String body)
      throws ImejiException {
    // Trim to avoid errors due to unwanted spaces
    doiServiceUrl = doiServiceUrl.trim();
    validateURL(doiServiceUrl);

    final Response response = client.target(doiServiceUrl + "/" + doi).queryParam("url", url)
        .register(HttpAuthenticationFeature.basic(doiUser, doiPassword)).register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.APPLICATION_XML).post(Entity.entity(body, "text/xml"));

    final int statusCode = response.getStatus();
    final String responseEntity = response.readEntity(String.class);

    if (statusCode == HttpStatus.SC_CREATED) {
      LOGGER.info("DOI {} sucessfully updated by DOXI.", doi);
    } else {
      LOGGER.error("Error occured, when contacting DOXI. StatusCode = {} - Parameters: DOI = {}, URL = {}, Body = \n{}", statusCode, doi,
          url, body);
      throw new ImejiException("Error updatig DOI. StatusCode=" + statusCode + " - " + HttpStatus.getStatusText(statusCode) + " - "
          + responseEntity + ". Please contact your admin!");
    }

    return responseEntity;
  }

  private static void validateURL(String doiServiceUrl) throws ImejiException {
    try {
      new URL(doiServiceUrl);
    } catch (final MalformedURLException e) {
      throw new ImejiException("DOI Service: Invalid Service URL: " + e.getMessage());
    }
  }

}
