package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;

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
  private URI uri;
  @j2jLiteral("http://imeji.org/terms/statement")
  private String statementId;
  @j2jLiteral("http://imeji.org/terms/text")
  private String text;
  @j2jLiteral("http://imeji.org/terms/number")
  private double number;
  @j2jLiteral("http://imeji.org/terms/url")
  private String url;
  @j2jLiteral("http://imeji.org/terms/latitude")
  private double latitude;
  @j2jLiteral("http://imeji.org/terms/longitude")
  private double longitude;
  @j2jLiteral("http://xmlns.com/foaf/0.1/person")
  private Person person;
  @j2jLiteral("http://imeji.org/terms/pos")
  private int position = 0;

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
  public String getStatementId() {
    return statementId;
  }

  /**
   * @param statementId the statementId to set
   */
  public void setStatementId(String statementId) {
    this.statementId = statementId;
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

  /**
   * @return the position
   */
  public int getPosition() {
    return position;
  }

  /**
   * @param position the position to set
   */
  public void setPosition(int position) {
    this.position = position;
  }

}
