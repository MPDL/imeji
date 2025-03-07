package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonIgnore;
import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.model.util.HibernateURIConverter;
import de.mpg.imeji.logic.util.IdentifierUtil;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.util.DateHelper;
import jakarta.persistence.Convert;

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
  //@Convert(converter = HibernateURIConverter.class)
  private URI id;
  @j2jLiteral("http://imeji.org/terms/label")
  private String label;
  @j2jLiteral("http://imeji.org/terms/name")
  private String name;
  @j2jLiteral("http://imeji.org/terms/url")
  private String url;
  @j2jLiteral("http://imeji.org/terms/start")
  private long start = -1;
  @j2jLiteral("http://imeji.org/terms/end")
  private long end = -1;

  public License() {
    this.id = IdentifierUtil.newURI(License.class);
  }

  @Override
  public License clone() {
    final License clone = new License();
    clone.setId(id);
    clone.setLabel(label);
    clone.setName(name);
    clone.setEnd(end);
    clone.setStart(start);
    clone.setUrl(url);
    return clone;
  }

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass())
      return false;

    License license = (License) o;
    return start == license.start && end == license.end && Objects.equals(id, license.id) && Objects.equals(label, license.label)
        && Objects.equals(name, license.name) && Objects.equals(url, license.url);
  }

  @Override
  public int hashCode() {
    int result = Objects.hashCode(id);
    result = 31 * result + Objects.hashCode(label);
    result = 31 * result + Objects.hashCode(name);
    result = 31 * result + Objects.hashCode(url);
    result = 31 * result + Long.hashCode(start);
    result = 31 * result + Long.hashCode(end);
    return result;
  }

  /*
  @Override
  public boolean equals(Object obj) {
    if (!(obj.getClass().equals(this.getClass()))) {
      return false;
    }
    License o = (License) obj;
    if (!o.getId().equals(id)) {
      return false;
    }
    if (!o.getLabel().equals(label)) {
      return false;
    }
    if (!o.getName().equals(name)) {
      return false;
    }
    if (!o.getUrl().equals(url)) {
      return false;
    }
    if (o.getStart() != start) {
      return false;
    }
    if (o.getEnd() != end) {
      return false;
    }
    return true;
  }
  
   */

  public License(ImejiLicenses lic) {
    this.name = lic.name();
    this.url = lic.getUrl();
    this.label = lic.getLabel();
  }

  /**
   * Return the timestamp of the license as a string
   *
   * @return
   */
  @JsonIgnore
  public String getTimestamp() {
    final String s = start > 0 ? DateHelper.printDate(DateHelper.getDate(start)) : "...";
    final String e = end > 0 ? DateHelper.printDate(DateHelper.getDate(end)) : "...";
    return s + " - " + e;
  }

  @JsonIgnore
  public String getStartTime() {
    return start > 0 ? DateHelper.printDate(DateHelper.getDate(start)) : null;
  }

  @JsonIgnore
  public String getEndTime() {
    return end > 0 ? DateHelper.printDate(DateHelper.getDate(end)) : null;
  }

  /**
   * True if this license is emtpy
   *
   * @return
   */
  @JsonIgnore
  public boolean isEmtpy() {
    return StringHelper.isNullOrEmptyTrim(name) && StringHelper.isNullOrEmptyTrim(url);
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

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @param label the label to set
   */
  public void setLabel(String label) {
    this.label = label;
  }

}
