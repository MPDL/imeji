package de.mpg.imeji.logic.model.factory;

import java.net.URI;

import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.UploadResult;

/**
 * Factory for {@link ContentVO}
 * 
 * @author saquet
 *
 */
public class ContentFactory {
  private ContentVO content = new ContentVO();

  public ContentVO build() {
    return content;
  }

  /**
   * Initialize the Factory with a {@link ContentVO}
   * 
   * @param content
   * @return
   */
  public ContentFactory init(ContentVO content) {
    this.content = content;
    return this;
  }

  /**
   * Set all the Files from an {@link UploadResult}
   * 
   * @param uploadResult
   * @return
   */
  public ContentFactory setFiles(UploadResult uploadResult) {
    content.setPreview(uploadResult.getWeb());
    content.setThumbnail(uploadResult.getThumb());
    content.setFull(uploadResult.getFull());
    content.setOriginal(uploadResult.getFull());
    return this;
  }

  public ContentFactory setId(String id) {
    content.setId(URI.create(id));
    return this;
  }

  public ContentFactory setItemId(String itemId) {
    content.setItemId(itemId);
    return this;
  }

  public ContentFactory setOriginal(String url) {
    content.setOriginal(url);
    return this;
  }
}
