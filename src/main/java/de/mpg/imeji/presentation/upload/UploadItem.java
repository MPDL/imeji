package de.mpg.imeji.presentation.upload;

import java.io.Serializable;

import de.mpg.imeji.logic.vo.Item;

/**
 * Item for the Upload
 * 
 * @author saquet
 *
 */
public class UploadItem implements Serializable {
  private static final long serialVersionUID = 5046381115933485144L;
  private final String filename;
  private final String id;
  private final String idString;

  public UploadItem(Item item) {
    this.filename = item.getFilename();
    this.id = item.getId().toString();
    this.idString = item.getIdString();
  }

  public String getId() {
    return id;
  }

  public String getFilename() {
    return filename;
  }

  public String getIdString() {
    return idString;
  }
}
