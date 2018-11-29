package de.mpg.imeji.logic.doi.models;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class DOITitle implements Serializable {
  private static final long serialVersionUID = -5742761065218755681L;
  private String title;

  public DOITitle() {

  }

  public DOITitle(String title) {
    this.title = title;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }
}
