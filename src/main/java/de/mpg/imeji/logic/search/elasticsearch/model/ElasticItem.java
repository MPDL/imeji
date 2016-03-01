package de.mpg.imeji.logic.search.elasticsearch.model;

import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;

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
  private final String filetype;
  private final long size;
  private final String space;
  private final String checksum;
  private final long width;
  private final long height;
  private final List<ElasticMetadata> metadata = new ArrayList<>();
  private final List<String> album;


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
    this.filetype = item.getFiletype();
    this.height = item.getHeight();
    this.width = item.getWidth();
    this.album = item.getAlbums();
    this.size = item.getFileSize();
    this.space = space;
    for (Metadata md : item.getMetadataSet().getMetadata()) {
      metadata.add(new ElasticMetadata(md));
    }
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
   * @return the albums
   */
  public List<String> getAlbum() {
    return album;
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
}
