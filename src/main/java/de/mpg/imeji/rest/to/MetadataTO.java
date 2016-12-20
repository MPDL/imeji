package de.mpg.imeji.rest.to;

import java.io.Serializable;

import de.mpg.imeji.logic.vo.Metadata;

/**
 * TO for {@link Metadata}
 * 
 * @author saquet
 *
 */
public class MetadataTO implements Serializable {
  private static final long serialVersionUID = 1852898329656765285L;
  private String statementId;
  private String text;
  private double number;
  private String url;
  private double latitude;
  private double longitude;
  private PersonTO person;

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
  public PersonTO getPerson() {
    return person;
  }

  /**
   * @param person the person to set
   */
  public void setPerson(PersonTO person) {
    this.person = person;
  }



}
