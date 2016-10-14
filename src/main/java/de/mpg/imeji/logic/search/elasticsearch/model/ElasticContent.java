package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.ContentVO;
import de.mpg.imeji.logic.vo.TechnicalMetadata;

/**
 * Elastic Object for contentVO
 * 
 * @author saquet
 *
 */
public class ElasticContent {
  private final String filetype;
  private final long size;
  private final String checksum;
  private final long width;
  private final long height;
  private final List<ElasticTechnicalMetadata> technical = new ArrayList<>();
  private final String fulltext;

  /**
   * Constructor
   * 
   * @param contentVO
   */
  public ElasticContent(ContentVO contentVO) {
    this.filetype = contentVO.getMimetype();
    this.size = contentVO.getFileSize();
    this.height = contentVO.getHeight();
    this.width = contentVO.getWidth();
    this.fulltext = contentVO.getFulltext();
    this.checksum = contentVO.getChecksum();
    for (TechnicalMetadata md : contentVO.getTechnicalMetadata()) {
      technical.add(new ElasticTechnicalMetadata(md));
    }
  }

  /**
   * @return the filetype
   */
  public String getFiletype() {
    return filetype;
  }

  /**
   * @return the size
   */
  public long getSize() {
    return size;
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
   * @return the technical
   */
  public List<ElasticTechnicalMetadata> getTechnical() {
    return technical;
  }

  /**
   * @return the fulltext
   */
  public String getFulltext() {
    return fulltext;
  }


}
