package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.util.LicenseUtil;
import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.TechnicalMetadata;
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
  private String folder;
  private String filename;
  private String license;
  private String filetype;
  private long size;
  private String space;
  private String checksum;
  private long width;
  private long height;
  private List<ElasticMetadata> metadata = new ArrayList<>();
  private List<ElasticTechnicalMetadata> technical = new ArrayList<>();
  private String fulltext;


  /**
   * Constructor with an {@link Item}
   *
   * @param item
   */
  public ElasticItem(Item item, String space, ContentVO contentVO) {
    super(item);
    this.checksum = item.getChecksum();
    this.folder = item.getCollection().toString();
    this.filename = item.getFilename();
    this.filetype = item.getFiletype();
    this.size = item.getFileSize();
    this.space = space;
    this.license = getLicenseName(item);
    for (Metadata md : item.getMetadataSet().getMetadata()) {
      metadata.add(new ElasticMetadata(md));
    }
    copyContentVO(contentVO);
  }

  /**
   * Constructor used for partial update of contentVO
   * 
   * @param contentVO
   */
  public ElasticItem(ContentVO contentVO) {
    super(null);
    copyContentVO(contentVO);
  }

  private void copyContentVO(ContentVO contentVO) {
    if (contentVO != null) {
      this.height = contentVO.getHeight();
      this.width = contentVO.getWidth();
      this.fulltext = contentVO.getFulltext();
      for (TechnicalMetadata md : contentVO.getTechnicalMetadata()) {
        technical.add(new ElasticTechnicalMetadata(md));
      }
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
   * @return the width
   */
  public long getWidth() {
    return width;
  }

  /**
   * @return the height
   */
  public long getHeight() {
    return height;
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

  public String getFulltext() {
    return fulltext;
  }

  public List<ElasticTechnicalMetadata> getTechnical() {
    return technical;
  }

  public String getLicense() {
    return license;
  }
}
