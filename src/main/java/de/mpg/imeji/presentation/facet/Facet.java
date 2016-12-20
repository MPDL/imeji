/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.facet;

import java.net.URI;
import java.util.Locale;

import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.presentation.beans.MetadataLabels;

/**
 * The Facet used by the Faceted search in the browse item page
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class Facet {
  private URI uri;
  protected String label;
  protected int count;
  private FacetType type;
  protected URI metadataURI;
  protected String internationalizedLabel;
  protected MetadataLabels metadataLabels;

  /**
   * The type of the {@link Facet}. Depends on which page is displayed the {@link Facet}
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public enum FacetType {
    TECHNICAL, COLLECTION, SEARCH;
  }


  /**
   * Constructor for a {@link Facet}
   *
   * @param uri
   * @param label
   * @param count
   * @param type
   * @param metadataURI
   */
  public Facet(URI uri, String label, int count, FacetType type, URI metadataURI, Locale locale,
      MetadataLabels metadataLabels) {
    this.count = count;
    this.label = label;
    this.uri = uri;
    this.type = type;
    this.metadataURI = metadataURI;
    this.metadataLabels = metadataLabels;
    initInternationalLabel(locale);
  }

  /**
   * Initialized the internationalized label according to the {@link MetadataProfile}
   */
  private void initInternationalLabel(Locale locale) {
    if (FacetType.TECHNICAL.name().equals(type.name())) {
      internationalizedLabel =
          Imeji.RESOURCE_BUNDLE.getLabel("facet_" + label.toLowerCase(), locale);
    } else if (FacetType.COLLECTION.name().equals(type.name())) {
      internationalizedLabel = metadataLabels.getInternationalizedLabels().get(metadataURI);
    } else if (FacetType.SEARCH.name().equals(type.name())) {
      internationalizedLabel = Imeji.RESOURCE_BUNDLE.getLabel("search", locale);
    }
    if (internationalizedLabel == null
        || (label != null && internationalizedLabel.equals("facet_" + label.toLowerCase()))) {
      internationalizedLabel = label;
    }
  }

  /**
   * Getter
   *
   * @return
   */
  public URI getUri() {
    return uri;
  }

  /**
   * Setter
   *
   * @param uri
   */
  public void setUri(URI uri) {
    this.uri = uri;
  }

  /**
   * Getter
   *
   * @return
   */
  public String getinternationalizedLabel() {
    return internationalizedLabel;
  }

  /**
   * Getter
   *
   * @return
   */
  public String getLabel() {
    return label;
  }

  /**
   * Setter
   *
   * @param label
   */
  public void setLabel(String label) {
    this.label = label;
  }

  /**
   * Getter
   *
   * @return
   */
  public int getCount() {
    return count;
  }

  /**
   * Setter
   *
   * @param count
   */
  public void setCount(int count) {
    this.count = count;
  }

  /**
   * Getter
   *
   * @return
   */
  public FacetType getType() {
    return type;
  }

  /**
   * Setter
   *
   * @param type
   */
  public void setType(FacetType type) {
    this.type = type;
  }

  /**
   * Getter
   *
   * @return
   */
  public URI getMetadataURI() {
    return metadataURI;
  }

  /**
   * Setter
   *
   * @param metadataURI
   */
  public void setMetadataURI(URI metadataURI) {
    this.metadataURI = metadataURI;
  }

  /**
   * True if the current {@link Facet} is a negation facet
   *
   * @return
   */
  public boolean isNotDefine() {
    if (label == null) {
      return false;
    }
    return (label.toLowerCase().startsWith("no "));
  }

  /**
   * @return
   */
  public String getNotDefineType() {
    return metadataLabels.getInternationalizedLabels().get(metadataURI);
  }

}
