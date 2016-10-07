package de.mpg.imeji.rest.to;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlRootElement
@XmlType(propOrder = {"language", "value"})
public class LicenseTO implements Serializable {
  private static final long serialVersionUID = -6942964122324055238L;
  private String label;
  private String name;
  private String url;
  private String start;
  private String end;

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
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
   * @return the start
   */
  public String getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public void setStart(String start) {
    this.start = start;
  }

  /**
   * @return the end
   */
  public String getEnd() {
    return end;
  }

  /**
   * @param end the end to set
   */
  public void setEnd(String end) {
    this.end = end;
  }



}
