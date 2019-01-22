package de.mpg.imeji.util;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Java representation for rdf label with a lang property
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@XmlRootElement(name = "label", namespace = "http://www.w3.org/2000/01/rdf-schema#")
public class LocalizedString implements Serializable {
  private static final long serialVersionUID = 6853407399087925507L;
  private String value;
  private String lang;

  /**
   * Default constructor
   */
  public LocalizedString() {}

  /**
   * New {@link LocalizedString} with a {@link String} in one language
   *
   * @param value
   * @param lang
   */
  public LocalizedString(String value, String lang) {
    this.value = value;
    this.lang = lang;
  }

  /**
   * getter
   *
   * @return
   */
  @XmlAttribute(name = "value")
  public String getValue() {
    return value;
  }

  /**
   * setter
   *
   * @param value
   */
  public void setValue(String value) {
    this.value = value;
  }

  /**
   * getter
   *
   * @return
   */
  @XmlAttribute(name = "lang")
  public String getLang() {
    // Set en as default language
    if (lang == null || lang.equals("")) {
      lang = "en";
    }
    return lang;
  }

  /**
   * setter
   *
   * @param lang
   */
  public void setLang(String lang) {
    this.lang = lang;
  }
}
