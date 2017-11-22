package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.util.IdentifierUtil;

@j2jResource("http://imeji.org/terms/subscription")
@j2jModel("subscription")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Subscription implements Serializable {
  private static final long serialVersionUID = 3472025966712569075L;

  public enum Type {
    DONWLOAD, UPLOAD;
  }

  private URI id = IdentifierUtil.newURI(Subscription.class, "universal");
  // The type of the subscription
  @j2jLiteral("http://imeji.org/terms/subscriptionType")
  private String type;
  // The id of the user who subscribe
  @j2jLiteral("http://imeji.org/terms/userId")
  private String userId;
  // The id of the object which is observed
  @j2jLiteral("http://imeji.org/terms/objectId")
  private String objectId;

  /**
   * @return the type
   */
  public Type getType() {
    return Type.valueOf(type);
  }

  /**
   * @param type the type to set
   */
  public void setType(Type type) {
    this.type = type.name();
  }

  /**
   * @return the userId
   */
  public String getUserId() {
    return userId;
  }

  /**
   * @param userId the userId to set
   */
  public void setUserId(String userId) {
    this.userId = userId;
  }

  /**
   * @return the objectId
   */
  public String getObjectId() {
    return objectId;
  }

  /**
   * @param objectId the objectId to set
   */
  public void setObjectId(String objectId) {
    this.objectId = objectId;
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
