package de.mpg.imeji.logic.model;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * imeji collection has one {@link MetadataProfile} and contains {@link Item}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/collection")
@j2jModel("collection")
@j2jId(getMethod = "getId", setMethod = "setId")
public class CollectionImeji extends BasisMetadata implements Serializable {
  private static final long serialVersionUID = -4689209760815149573L;
  @j2jLiteral("http://imeji.org/terms/doi")
  private String doi;
  @j2jResource("http://imeji.org/terms/logoUrl")
  private URI logoUrl;
  @j2jLiteral("http://imeji.org/terms/statements")
  private String statements;

  private Collection<URI> images = new ArrayList<URI>();

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

  public void setImages(Collection<URI> images) {
    this.images = images;
  }

  public Collection<URI> getImages() {
    return images;
  }

  public String getStatements() {
    return statements;
  }

  public void setStatements(String statements) {
    this.statements = statements;
  }
}
