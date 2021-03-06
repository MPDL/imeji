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
  /**
   * Indicates whether the facet is part of an item or an collection
   */
  @j2jLiteral("http://imeji.org/terms/objecttype")
  private String objectType = OBJECTTYPE_ITEM;
  private URI uri = IdentifierUtil.newURI(Facet.class);
  @j2jLiteral("http://imeji.org/terms/position")
  private int position = 0;


  public static final String OBJECTTYPE_ITEM = "item";
  public static final String OBJECTTYPE_COLLECTION = "collection";

  /**
   * Reserved Facet Name to count all items of a collection and its subcollections
   */
  public static final String ITEMS = "count_all_items";
  /**
   * Reserved Facet Name to count all elements (items + subcollection) of a collection (without the
   * content of the subcollections)
   */
  public static final String COLLECTION_ITEMS = "count_all_collection_items";
  /**
   * Reserved Facet name to count all subcollections of a collection and its subcollections
   */
  public static final String SUBCOLLECTIONS = "count_all_collection_subcollections";

  /**
   * Reserved Facet Name to count all (root) items of a collection (without the items of the
   * subcollections)
   */
  public static final String COLLECTION_ROOT_ITEMS = "count_all_collection_root_items";

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

  public String getObjectType() {
    return objectType;
  }

  public void setObjectType(String objectType) {
    this.objectType = objectType;
  }

}
