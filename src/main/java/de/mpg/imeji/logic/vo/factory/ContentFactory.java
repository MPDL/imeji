package de.mpg.imeji.logic.vo.factory;

import java.net.URI;

import de.mpg.imeji.logic.vo.ContentVO;

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

  public ContentFactory setId(String id) {
    content.setId(URI.create(id));
    return this;
  }

}
