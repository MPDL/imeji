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
import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.doi.models.DOICollection;
import de.mpg.imeji.logic.doi.models.DOICreators;
import de.mpg.imeji.logic.doi.models.DOIIdentifier;
import de.mpg.imeji.logic.doi.models.DOITitle;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Person;

/**
 * Utility Class for DOI Service
 *
 * @author bastiens
 *
 */
public class DOIUtil {
  private static final Client client = ClientBuilder.newClient();

  private DOIUtil() {
    // private controller
  }

  public static DOICollection transformToDO(CollectionImeji col) {
    DOICollection dcol = new DOICollection();
    DOITitle title = new DOITitle(col.getMetadata().getTitle());
    List<DOICreators> creators = new ArrayList<DOICreators>();
    for (Person author : col.getMetadata().getPersons()) {
      creators.add(new DOICreators(author.getCompleteName()));
    }

    dcol.setIdentifier(new DOIIdentifier());
    dcol.getTitles().add(title);
    dcol.setPublisher("MPG");
    dcol.setCreators(creators);
    dcol.setPublicationYear(String.valueOf(col.getCreated().get(Calendar.YEAR)));

    return dcol;
  }

  public static String convertToXML(DOICollection dcol) throws ImejiException {
    StringWriter sw = new StringWriter();

    try {
      JAXBContext context = JAXBContext.newInstance(DOICollection.class);
      Marshaller m;
      m = context.createMarshaller();
      m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
      m.setProperty(Marshaller.JAXB_SCHEMA_LOCATION,
          "http://datacite.org/schema/kernel-3 http://schema.datacite.org/meta/kernel-3/metadata.xsd");
      m.marshal(dcol, sw);
    } catch (JAXBException e) {
      throw new ImejiException("Error occured, when contacting DOxI.");
    }

    String xml = sw.toString().trim();
    return xml;

  }

  public static String makeDOIRequest(String doiServiceUrl, String doiUser, String doiPassword,
      String url, String xml) throws ImejiException {

    // Trim to avoid errors due to unwanted spaces
    doiServiceUrl = doiServiceUrl.trim();
    validateURL(doiServiceUrl);

    Response response = client.target(doiServiceUrl).queryParam("url", url)
        .register(HttpAuthenticationFeature.basic(doiUser, doiPassword))
        .register(MultiPartFeature.class).register(JacksonFeature.class)
        .request(MediaType.TEXT_PLAIN).put(Entity.entity(xml, "text/xml"));

    int statusCode = response.getStatus();

    // throw Exception if the DOI service request fails
    if (statusCode != HttpStatus.SC_CREATED) {
      throw new ImejiException("Error occured, when contacting DOxI. StatusCode=" + statusCode
          + " - " + HttpStatus.getStatusText(statusCode) + " - " + response.readEntity(String.class)
          + ". Please contact your admin");
    }
    return response.readEntity(String.class);
  }

  private static void validateURL(String doiServiceUrl) throws ImejiException {
    try {
      new URL(doiServiceUrl);
    } catch (MalformedURLException e) {
      throw new ImejiException("DOI Service: Invalid Service URL: " + e.getMessage());
    }
  }

}
