package de.mpg.imeji.logic.vo;

import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyList;
import de.mpg.imeji.j2j.annotations.j2jLazyLiteral;
import de.mpg.imeji.j2j.annotations.j2jLiteral;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;
import de.mpg.imeji.logic.ImejiNamespaces;

/**
 * Content of an Item
 * 
 * @author saquet
 *
 */
@j2jResource("http://imeji.org/terms/content")
@j2jModel("content")
@j2jId(getMethod = "getId", setMethod = "setId")
public class ContentVO implements Serializable {
  private static final long serialVersionUID = -7906584876989077898L;
  private URI id;
  @j2jLazyLiteral("http://imeji.org/terms/fulltext")
  private String fulltext;
  @j2jLazyList(ImejiNamespaces.TECHNICAL_METADATA)
  private List<TechnicalMetadata> technicalMetadata = new ArrayList<>();
  @j2jLiteral("http://imeji.org/terms/thumbnail")
  private String thumbnail;
  @j2jLiteral("http://imeji.org/terms/preview")
  private String preview;
  @j2jLiteral("http://imeji.org/terms/original")
  private String original;
  @j2jLiteral("http://imeji.org/terms/mimetype")
  private String mimetype;
  @j2jLiteral("http://imeji.org/terms/checksum")
  private String checksum;
  @j2jLiteral("http://imeji.org/terms/fileSize")
  private long fileSize;
  @j2jLiteral("http://www.w3.org/2003/12/exif/ns#width")
  private long width;
  @j2jLiteral("http://www.w3.org/2003/12/exif/ns#height")
  private long height;

  public ContentVO() {

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
   * @return the fulltext
   */
  public String getFulltext() {
    return fulltext;
  }

  /**
   * @param fulltext the fulltext to set
   */
  public void setFulltext(String fulltext) {
    this.fulltext = fulltext;
  }

  /**
   * @return the technicalMetadata
   */
  public List<TechnicalMetadata> getTechnicalMetadata() {
    return technicalMetadata;
  }

  /**
   * @param technicalMetadata the technicalMetadata to set
   */
  public void setTechnicalMetadata(List<TechnicalMetadata> technicalMetadata) {
    this.technicalMetadata = technicalMetadata;
  }

  /**
   * @return the thumbnail
   */
  public String getThumbnail() {
    return thumbnail;
  }

  /**
   * @param thumbnail the thumbnail to set
   */
  public void setThumbnail(String thumbnail) {
    this.thumbnail = thumbnail;
  }

  /**
   * @return the preview
   */
  public String getPreview() {
    return preview;
  }

  /**
   * @param preview the preview to set
   */
  public void setPreview(String preview) {
    this.preview = preview;
  }

  /**
   * @return the original
   */
  public String getOriginal() {
    return original;
  }

  /**
   * @param original the original to set
   */
  public void setOriginal(String original) {
    this.original = original;
  }

  /**
   * @return the checksum
   */
  public String getChecksum() {
    return checksum;
  }

  /**
   * @param checksum the checksum to set
   */
  public void setChecksum(String checksum) {
    this.checksum = checksum;
  }

  /**
   * @return the fileSize
   */
  public long getFileSize() {
    return fileSize;
  }

  /**
   * @param fileSize the fileSize to set
   */
  public void setFileSize(long fileSize) {
    this.fileSize = fileSize;
  }

  /**
   * @return the width
   */
  public long getWidth() {
    return width;
  }

  /**
   * @param width the width to set
   */
  public void setWidth(long width) {
    this.width = width;
  }

  /**
   * @return the height
   */
  public long getHeight() {
    return height;
  }

  /**
   * @param height the height to set
   */
  public void setHeight(long height) {
    this.height = height;
  }

  /**
   * @return the mimetype
   */
  public String getMimetype() {
    return mimetype;
  }

  /**
   * @param mimetype the mimetype to set
   */
  public void setMimetype(String mimetype) {
    this.mimetype = mimetype;
  }

}
