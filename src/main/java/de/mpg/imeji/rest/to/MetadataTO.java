package de.mpg.imeji.rest.to;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import de.mpg.imeji.logic.vo.Metadata;

/**
 * TO for {@link Metadata}
 *
 * @author saquet
 *
 */
@JsonInclude(Include.NON_DEFAULT)
public class MetadataTO implements Serializable {
  private static final long serialVersionUID = 1852898329656765285L;
  private String index;
  private String text = "";
  private String date = "";
  private String title = "";
  private String name = "";
  private double number = Double.NaN;
  private String url = "";
  private double latitude = Double.NaN;
  private double longitude = Double.NaN;
  private PersonTO person;

  /**
   * @return the statementId
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param statementId the statementId to set
   */
  public void setIndex(String index) {
    this.index = index;
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

  /**
   * @return the date
   */
  public String getDate() {
    return date;
  }

  /**
   * @param date the date to set
   */
  public void setDate(String date) {
    this.date = date;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

}
