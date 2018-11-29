package de.mpg.imeji.rest.to;

import javax.ws.rs.core.Response.Status;

public class JSONResponse {
  public Object object;
  public Status status;
  public int statusCode;

  public JSONResponse() {
    statusCode = -1;

  }

  public Object getObject() {
    return object;
  }

  public void setObject(Object object) {
    this.object = object;
  }

  public int getStatus() {
    return statusCode > 0 ? statusCode : status.getStatusCode();
  }

  public void setStatus(Status status) {
    this.status = status;
  }

  public void setStatus(int statusCode) {
    this.statusCode = statusCode;
  }

}
