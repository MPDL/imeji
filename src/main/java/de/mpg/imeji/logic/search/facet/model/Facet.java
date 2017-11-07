package de.mpg.imeji.logic.search.facet.model;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

@j2jModel("facet")
@j2jId(getMethod = "getUri", setMethod = "setUri")
@j2jResource("http://imeji.org/terms/facet")
public class Facet implements Serializable {
  private static final long serialVersionUID = 574256459220027826L;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;
  @j2jLiteral("http://imeji.org/terms/type")
  private String type;
  @j2jLiteral("http://imeji.org/terms/index")
  private String index;
  private URI uri = IdentifierUtil.newURI(Facet.class);
  @j2jLiteral("http://imeji.org/terms/position")
  private int position = 0;

  /**
   * Reserved Facet Name to count the items of a collection
   */
  public static final String ITEMS = "count_all_collection_items";
  /**
   * Reserved Facet name to count the subcollections of a collection
   */
  public static final String SUBCOLLECTIONS = "count_all_collection_subcollections";


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
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the index
   */
  public String getIndex() {
    return index;
  }

  /**
   * @param index the index to set
   */
  public void setIndex(String index) {
    this.index = index;
  }

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

  public String getIdString() {
    if (uri != null) {
      return uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1);
    }
    return "";
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

}
