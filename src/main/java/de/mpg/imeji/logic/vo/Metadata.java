package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

/**
 * A metadata for imeji object
 *
 * @author saquet
 *
 */
@j2jResource("http://imeji.org/terms/metadata")
@j2jId(getMethod = "getUri", setMethod = "setUri")
public class Metadata implements Serializable {
  private static final long serialVersionUID = 8758936270562178555L;
  private URI uri = IdentifierUtil.newURI(Metadata.class, "universal");
  @j2jLiteral("http://imeji.org/terms/statement")
  private String index;
  @j2jLiteral("http://imeji.org/terms/text")
  private String text = null;
  @j2jLiteral("http://imeji.org/terms/number")
  private double number = Double.NaN;
  @j2jLiteral("http://imeji.org/terms/url")
  private String url = null;
  @j2jLiteral("http://imeji.org/terms/latitude")
  private double latitude = Double.NaN;
  @j2jLiteral("http://imeji.org/terms/longitude")
  private double longitude = Double.NaN;
  @j2jLiteral("http://xmlns.com/foaf/0.1/person")
  private Person person;

  public Metadata copy() {
    Metadata copy = new Metadata();
    copy.setIndex(index);
    copy.setText(text);
    copy.setNumber(number);
    copy.setUrl(url);
    if (person != null) {
      copy.setPerson(person.clone());
    }
    copy.setLongitude(longitude);
    copy.setLatitude(latitude);
    return copy;
  }

  /**
   * @return the uri
   */
  public URI getUri() {
    return uri;
  }

  /**
   * @param uri the uri to set
   */
  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
   * @return the statementId
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param statementId the statementId to set
   */
  public void setIndex(String statementId) {
    this.index = statementId;
  }

  /**
   * @return the text
   */
  public String getText() {
    return text;
  }

  /**
   * @param text the text to set
   */
  public void setText(String text) {
    this.text = text;
  }

  /**
   * @return the number
   */
  public double getNumber() {
    return number;
  }

  /**
   * @param number the number to set
   */
  public void setNumber(double number) {
    this.number = number;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the latitude
   */
  public double getLatitude() {
    return latitude;
  }

  /**
   * @param latitude the latitude to set
   */
  public void setLatitude(double latitude) {
    this.latitude = latitude;
  }

  /**
   * @return the longitude
   */
  public double getLongitude() {
    return longitude;
  }

  /**
   * @param longitude the longitude to set
   */
  public void setLongitude(double longitude) {
    this.longitude = longitude;
  }

  /**
   * @return the person
   */
  public Person getPerson() {
    return person;
  }

  /**
   * @param person the person to set
   */
  public void setPerson(Person person) {
    this.person = person;
  }


}
