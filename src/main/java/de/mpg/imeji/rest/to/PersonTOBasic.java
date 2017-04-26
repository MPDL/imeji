package de.mpg.imeji.rest.to;

import java.io.Serializable;


public class PersonTOBasic implements Serializable {
  private static final long serialVersionUID = 306199590952021003L;

  private String fullname;

  private String userId;

  public PersonTOBasic() {

  }

  public PersonTOBasic(String fullname, String userId) {
    this.fullname = fullname;
    this.userId = userId;
  }

  public String getFullname() {
    return fullname;
  }

  public void setFullname(String fullname) {
    this.fullname = fullname;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }



}
