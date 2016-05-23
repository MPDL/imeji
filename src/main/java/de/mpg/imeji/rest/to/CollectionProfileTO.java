package de.mpg.imeji.rest.to;

import java.io.Serializable;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Created by vlad on 25.11.14.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class CollectionProfileTO implements Serializable {

  private static final long serialVersionUID = -5210147403244095642L;

  public static enum METHOD {
    REFERENCE, COPY;
    @Override
    public String toString() {
      return super.toString().toLowerCase();
    }
  }

  private String id;
  private String method;

  public String getId() {
    return id;
  }

  public void setId(String profileId) {
    this.id = profileId;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }
}
