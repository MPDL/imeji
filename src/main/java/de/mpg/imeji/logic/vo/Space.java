/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * imeji space
 *
 * @author vmakarenko (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/space")
@j2jModel("space")
@j2jId(getMethod = "getId", setMethod = "setId")
@XmlAccessorType(XmlAccessType.NONE)
@XmlRootElement(name = "space", namespace = "http://imeji.org/terms/")
public class Space extends Properties implements Serializable {

  private static final long serialVersionUID = 3531439142807036969L;

  @j2jLiteral("http://purl.org/dc/elements/1.1/title")
  private String title;
  @j2jLiteral("http://purl.org/dc/elements/1.1/description")
  private String description;
  @j2jResource("http://imeji.org/terms/logoUrl")
  private URI logoUrl;
  @j2jLiteral("http://imeji.org/terms/slug")
  private String slug;

  // @j2jList("http://imeji.org/terms/collection")
  private Collection<String> spaceCollections = new ArrayList<String>();


  @XmlElement(name = "logoUrl", namespace = "http://imeji.org/terms/")
  public URI getLogoUrl() {
    return logoUrl;
  }

  public void setLogoUrl(URI logoUrl) {
    this.logoUrl = logoUrl;
  }

  @XmlElement(name = "slug", namespace = "http://imeji.org/terms/")
  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  @XmlElement(name = "title", namespace = "http://imeji.org/terms/")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @XmlElement(name = "description", namespace = "http://imeji.org/terms/")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }


  /**
   * Getters and Setters below are only VO getters and setters, not written in JENA
   *
   */
  public Collection<String> getSpaceCollections() {
    return spaceCollections;
  }

  public String addSpaceCollection(String id) {
    if (!this.spaceCollections.contains(id)) {
      this.spaceCollections.add(id);
    }
    return id;
  }


  public void removeSpaceCollection(String id) {
    this.spaceCollections.remove(id);
  }


  public void setSpaceCollections(Collection<String> spaceCollections) {
    this.spaceCollections = spaceCollections;
  }

}
