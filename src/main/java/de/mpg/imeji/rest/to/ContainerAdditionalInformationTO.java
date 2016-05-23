package de.mpg.imeji.rest.to;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * TO for ContainerAdditionalInfo
 *
 * @author bastiens
 *
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ContainerAdditionalInformationTO implements Serializable {
  private static final long serialVersionUID = 8886559024173872540L;
  private String label;
  private String text;
  private String url;

  public ContainerAdditionalInformationTO(String label, String text, String url) {
    this.setLabel(label);
    this.setText(text);
    this.setUrl(url);
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


}
