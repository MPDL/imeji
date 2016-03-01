package de.mpg.imeji.logic;

import de.mpg.imeji.logic.auth.authorization.Authorization;

/**
 * An RDF Triple, which can be Checked by the {@link Authorization}
 * 
 * @author saquet
 *
 */
public class ImejiTriple {

  private String uri;
  private String property;
  private Object value;
  // The object which the triples belong to
  private Object object;

  /**
   * Default constructor
   * 
   * @param uri
   * @param property
   * @param value
   * @param securityUri
   */
  public ImejiTriple(String uri, String property, Object value, Object object) {
    this.uri = uri;
    this.property = property;
    this.value = value;
    this.object = object;
  }

  public String getUri() {
    return uri;
  }

  public void setUri(String uri) {
    this.uri = uri;
  }

  public String getProperty() {
    return property;
  }

  public void setProperty(String property) {
    this.property = property;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public Object getObject() {
    return object;
  }

  public void setObject(Object object) {
    this.object = object;
  }

}
