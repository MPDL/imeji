package de.mpg.imeji.logic.vo.util;

import java.net.URI;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.ConePerson;
import de.mpg.imeji.logic.vo.predefinedMetadata.Date;
import de.mpg.imeji.logic.vo.predefinedMetadata.Geolocation;
import de.mpg.imeji.logic.vo.predefinedMetadata.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Number;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.logic.vo.predefinedMetadata.Text;
import de.mpg.imeji.util.LocalizedString;

public class MetadataAndProfileHelper {

  private MetadataAndProfileHelper() {
    // private constructor
  }

  public static Metadata getDefaultValueMetadata(Statement statement)
      throws UnprocessableError, IllegalAccessException, InstantiationException {
    String type = statement.getType().toString();
    Metadata md = Metadata.createNewInstance(statement.getType());

    switch (Metadata.Types.valueOfUri(type)) {
      case TEXT:
        String textValue = "Textual value";
        Text newT = new Text();
        newT.setText(textValue);
        md = newT;
        break;
      case NUMBER:
        de.mpg.imeji.logic.vo.predefinedMetadata.Number newNT =
            new de.mpg.imeji.logic.vo.predefinedMetadata.Number(999.9);
        md = newNT;
        break;
      case CONE_PERSON:
        ConePerson newCone = new ConePerson();
        Person newP = new Person();
        newP.setFamilyName("Family name");
        newP.setGivenName("Given name");
        Organization org = new Organization();
        org.setName("Organization name");
        org.setDescription("Organization description");
        org.setCountry("Organization country");
        org.setCity("Organization city");
        newP.getOrganizations().add(org);
        newCone.setPerson(newP);
        md = newCone;
        break;
      case DATE:
        de.mpg.imeji.logic.vo.predefinedMetadata.Date newDT =
            new de.mpg.imeji.logic.vo.predefinedMetadata.Date();
        newDT.setDate("2015-01-01");
        md = newDT;
        break;
      case GEOLOCATION:
        Geolocation newGT = new Geolocation();
        newGT.setName("Munich");
        newGT.setLatitude(48.1333);
        newGT.setLongitude(11.5667);
        md = newGT;
        break;
      case LICENSE:
        License newLicense = new License();
        newLicense.setLicense("License name");
        newLicense.setExternalUri(URI.create("http://example.license.com"));
        md = newLicense;
        break;
      case LINK:
        Link newLink = new Link();
        newLink.setUri(URI.create("http://example.link.com"));
        newLink.setLabel("Link label");
        md = newLink;
        break;
      case PUBLICATION:
        Publication newPub = new Publication();
        newPub.setCitation("Citation string");
        md = newPub;
        break;
    }
    return md;
  }

  public static Statement getStatement(URI uri, MetadataProfile profile) {
    for (Statement st : profile.getStatements()) {
      if (st.getId().toString().equals(uri.toString())) {
        return st;
      }
    }
    return null;
  }


  /**
   * Return true if the {@link Metadata} has an empty value (which shouldn't be store in the
   * database)
   *
   * @param md
   * @return
   */
  public static boolean isEmpty(Metadata md) {
    if (md instanceof Text) {
      if (((Text) md).getText() == null || "".equals(((Text) md).getText().trim())) {
        return true;
      }
    } else if (md instanceof Date) {
      if (((Date) md).getDate() == null || "".equals(((Date) md).getDate())
          || Double.isNaN(((Date) md).getTime())) {
        return true;
      }
    } else if (md instanceof Geolocation) {
      Geolocation geo = (Geolocation) md;
      return StringHelper.isNullOrEmptyTrim(geo.getName()) && Double.isNaN(geo.getLatitude())
          && Double.isNaN(geo.getLongitude());
    } else if (md instanceof License) {
      if ((((License) md).getLicense() == null || "".equals(((License) md).getLicense().trim()))
          && ((License) md).getExternalUri() == null) {
        return true;
      }
    } else if (md instanceof Publication) {
      if (((Publication) md).getUri() == null
          || "".equals(((Publication) md).getUri().toString())) {
        return true;
      }
    } else if (md instanceof Number) {
      return Double.isNaN(((Number) md).getNumber());
    } else if (md instanceof ConePerson) {
      if (((ConePerson) md).getPerson() == null
          || (StringHelper.isNullOrEmptyTrim(((ConePerson) md).getPerson().AsFullText()))) {
        return true;
      }
    } else if (md instanceof Link) {
      if (StringHelper.isNullOrEmptyTrim(((Link) md).getUri())
          && StringHelper.isNullOrEmptyTrim(((Link) md).getLabel())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Checks if the Metadata statement with prvided URI is a parent at all
   *
   * @param profile - {@link MetadataProfile} object}
   * @param statement - the statement URI
   * @return true if the statement provided is parent in defined metadata profile
   */
  public static boolean isMetadataParent(URI statement, MetadataProfile profile) {
    for (Statement pS : profile.getStatements()) {
      if (statement.equals(pS.getParent())) {
        return true;
      }
    }
    return false;
  }


  public static Statement findStatementByLabel(String label, MetadataProfile profile)
      throws BadRequestException {
    for (Statement st : profile.getStatements()) {
      for (LocalizedString l : st.getLabels()) {
        if (l.getValue().equals(label)) {
          return st;
        }
      }
    }
    throw new BadRequestException("Metadata \"" + label + "\" not found!");
  }

}
