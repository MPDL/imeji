package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.util.LicenseUtil;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;

/**
 * The object which is indexed in Elastic search <br/>
 * !!! IMPORTANT !!!<br/>
 * This File must be synchronized with resources/elasticsearch/ElasticItemsMapping.json
 *
 * @author bastiens
 *
 */
public final class ElasticItem extends ElasticProperties {
  private final String folder;
  private final String filename;
  private final String license;
  private final String filetype;
  private final long size;
  private final String space;
  private final String checksum;
  private final List<ElasticMetadata> metadata = new ArrayList<>();


  /**
   * Constructor with an {@link Item}
   *
   * @param item
   */
  public ElasticItem(Item item, String space) {
    super(item);
    this.checksum = item.getChecksum();
    this.folder = item.getCollection().toString();
    this.filename = item.getFilename();
    this.space = space;
    this.license = getLicenseName(item);
    this.size = item.getFileSize();
    this.filetype = item.getFiletype();
    for (Metadata md : item.getMetadataSet().getMetadata()) {
      metadata.add(new ElasticMetadata(md));
    }
  }


  private String getLicenseName(Item item) {
    License license = LicenseUtil.getActiveLicense(item);
    return license != null ? license.getName() : null;
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  /**
   * @return the filetype
   */
  public String getFiletype() {
    return filetype;
  }

  /**
   * @return the checksum
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * @return the metadata
   */
  public List<ElasticMetadata> getMetadata() {
    return metadata;
  }

  /**
   * @return the folder
   */
  public String getFolder() {
    return folder;
  }

  /**
   * @return the space
   */
  public String getSpace() {
    return space;
  }

  public long getSize() {
    return size;
  }


  public String getLicense() {
    return license;
  }
}
