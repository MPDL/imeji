/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.Collection;

import javax.xml.bind.annotation.XmlElement;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyLiteral;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * Super class for all imeji containers ({@link CollectionImeji} and {@link Album})
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/container")
@j2jId(getMethod = "getId", setMethod = "setId")
public abstract class Container extends Properties implements Serializable {
  private static final long serialVersionUID = -5314334939747696614L;
  @j2jResource("http://imeji.org/terms/container/metadata")
  private ContainerMetadata metadata = new ContainerMetadata();
  @j2jLazyLiteral("http://imeji.org/terms/doi")
  private String doi;
  @j2jResource("http://imeji.org/terms/logoUrl")
  private URI logoUrl;


  public void setMetadata(ContainerMetadata metadata) {
    this.metadata = metadata;
  }

  public ContainerMetadata getMetadata() {
    return metadata;
  }

  public abstract void setImages(Collection<URI> images);

  public abstract Collection<URI> getImages();

  @XmlElement(name = "logoUrl", namespace = "http://imeji.org/terms/")
  public URI getLogoUrl() {
    return this.logoUrl;
  }

  public void setLogoUrl(URI logoUrl) {
    this.logoUrl = logoUrl;
  }

  public void setDoi(String doi) {
    this.doi = doi;
  }

  public String getDoi() {
    return doi;
  }
}
