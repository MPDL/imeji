package de.mpg.imeji.rest.to.predefinedMetadataTO;

import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import de.mpg.imeji.rest.to.predefinedMetadataTO.annotations.MetadataTOType;

@XmlRootElement
@MetadataTOType("http://imeji.org/terms/metadata#text")
@XmlType(propOrder = {"text"})
public class TextTO extends MetadataTO {
  private static final long serialVersionUID = -3794700986989015104L;
  private String text;

  public String getText() {
    return text;
  }

  public void setText(String text) {
    this.text = text;
  }
}
