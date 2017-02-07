package de.mpg.imeji.logic.vo.util;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Organization;
import de.mpg.imeji.logic.vo.Person;
import de.mpg.imeji.logic.vo.Statement;


public class MetadataUtil {

  private MetadataUtil() {
    // private constructor
  }

  public static Metadata getDefaultValueMetadata(Statement statement)
      throws UnprocessableError, IllegalAccessException, InstantiationException {
    final Metadata md = new Metadata();
    md.setStatementId(statement.getIndex());
    switch (statement.getType()) {
      case TEXT:
        md.setText("Textual value");
        break;
      case DATE:
        md.setText("2015-01-01");
        break;
      case NUMBER:
        md.setNumber(999.9);
      case GEOLOCATION:
        md.setText("Munich");
        md.setLatitude(48.1333);
        md.setLongitude(11.5667);
        break;
      case PERSON:
        final Person p = new Person();
        p.setFamilyName("Family name");
        p.setGivenName("Given name");
        final Organization org = new Organization();
        org.setName("Organization name");
        p.getOrganizations().add(org);
        md.setPerson(p);
        break;
      case URL:
        md.setText("Title");
        md.setUrl("http://example.org");
        break;
    }
    return md;
  }

  /**
   * Return true if the {@link Metadata} has an empty value (which shouldn't be store in the
   * database)
   *
   * @param md
   * @return
   */
  public static boolean isEmpty(Metadata md) {
    return StringHelper.isNullOrEmptyTrim(md.getText()) && Double.isNaN(md.getLatitude())
        && Double.isNaN(md.getLongitude()) && Double.isNaN(md.getNumber())
        && StringHelper.isNullOrEmptyTrim(md.getUrl()) && (md.getPerson() == null
            || StringHelper.isNullOrEmptyTrim(md.getPerson().getFamilyName()));
  }

  public static Statement findStatementByLabel(String label) {
    // TODO
    return null;
  }

  /**
   * Clean the metadata values
   *
   * @param md
   * @return
   */
  public static Metadata cleanMetadata(Metadata md) {
    md.setText(md.getText() != null ? md.getText().trim() : null);
    return md;
  }
}
