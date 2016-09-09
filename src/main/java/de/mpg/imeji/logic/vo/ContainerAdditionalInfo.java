package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * Additional Information for container metadata
 *
 * @author bastiens
 *
 */
@j2jResource("http://imeji.org/AdditionalInfo")
@j2jId(getMethod = "getId", setMethod = "setId")
public class ContainerAdditionalInfo implements Serializable {
  private static final long serialVersionUID = -1920870551000242907L;
  private URI id;// = IdentifierUtil.newURI(ContainerAdditionalInfo.class, "universal");
  @j2jLiteral("http://www.w3.org/2000/01/rdf-schema#label")
  private String label;
  @j2jLiteral("http://imeji.org/terms/text")
  private String text;
  @j2jLiteral("http://imeji.org/terms/uri")
  private String url;

  public ContainerAdditionalInfo() {
    // default constructor
  }

  public ContainerAdditionalInfo(String label, String text, String url) {
    this.label = label;
    this.text = text;
    this.url = url;
  }

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
   * @return the id
   */
  public URI getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(URI id) {
    this.id = id;
  }


}
