package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.util.DateHelper;

/**
 * A License for imeji objects
 * 
 * @author saquet
 *
 */
@j2jResource("http://imeji.org/terms/license")
@j2jId(getMethod = "getId", setMethod = "setId")
public class License implements Serializable {
  private static final long serialVersionUID = -966062330323435843L;
  private URI id;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;
  @j2jLiteral("http://imeji.org/terms/url")
  private String url;
  @j2jLiteral("http://imeji.org/terms/start")
  private long start = -1;
  @j2jLiteral("http://imeji.org/terms/end")
  private long end = -1;

  /**
   * Return the timestamp of the license as a string
   * 
   * @return
   */
  public String getTimestamp() {
    String s = start > 0 ? DateHelper.printDate(DateHelper.getDate(start)) : "...";
    String e = end > 0 ? DateHelper.printDate(DateHelper.getDate(end)) : "...";
    return s + " - " + e;
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
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @param url the url to set
   */
  public void setUrl(String url) {
    this.url = url;
  }

  /**
   * @return the start
   */
  public long getStart() {
    return start;
  }

  /**
   * @param start the start to set
   */
  public void setStart(long start) {
    this.start = start;
  }

  /**
   * @return the end
   */
  public long getEnd() {
    return end;
  }

  /**
   * @param end the end to set
   */
  public void setEnd(long end) {
    this.end = end;
  }

}
