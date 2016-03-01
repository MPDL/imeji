/**
 * 
 */
package de.mpg.imeji.logic.ingest.vo;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author hnguyen
 */
@XmlRootElement(name = "ingestProfile", namespace = "http://imeji.org/terms/")
public class IngestProfile {
  private MetadataProfiles metadataProfiles;
  private Items items;

  /**
	 * 
	 */
  public IngestProfile() {
    
  }

  /**
   * @return the mdProfile
   */
  @XmlElement(name = "metadataProfiles", namespace = "http://imeji.org/terms/")
  public MetadataProfiles getMetadataProfiles() {
    return metadataProfiles;
  }

  /**
   * @param mdProfile the mdProfile to set
   */
  public void setMetadataProfiles(MetadataProfiles metadataProfiles) {
    this.metadataProfiles = metadataProfiles;
  }

  /**
   * @return the items
   */
  @XmlElement(name = "items", namespace = "http://imeji.org/terms/")
  public Items getItems() {
    return items;
  }

  /**
   * @param items the items to set
   */
  public void setItems(Items items) {
    this.items = items;
  }
}
