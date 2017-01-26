package de.mpg.imeji.logic.concurrency.locks;

public class Lock {
  private long createTime = 0;
  private String email = null;
  private String uri = null;

  /**
   * Lock for one user (Used for GUI presentation)
   *
   * @param uri
   * @param email
   */
  public Lock(String uri, String email) {
    this.email = email;
    this.uri = uri.toString();
    initTime();
  }

  /**
   * Lock for all users (Used by system operation)
   *
   * @param uri
   */
  public Lock(String uri) {
    this.uri = uri.toString();
    initTime();
  }

  public void initTime() {
    createTime = System.currentTimeMillis();
  }

  public long getCreateTime() {
    return createTime;
  }

  public String getEmail() {
    return email;
  }

  public String getUri() {
    return uri;
  }
}
