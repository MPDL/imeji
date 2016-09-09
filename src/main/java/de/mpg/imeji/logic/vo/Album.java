/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.vo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;

import de.mpg.imeji.j2j.annotations.j2jId;
import de.mpg.imeji.j2j.annotations.j2jLazyList;
import de.mpg.imeji.j2j.annotations.j2jModel;
import de.mpg.imeji.j2j.annotations.j2jResource;

/**
 * Simple {@link Container} which can have {@link Item} from different {@link Collection}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@j2jResource("http://imeji.org/terms/album")
@j2jModel("album")
@j2jId(getMethod = "getId", setMethod = "setId")
public class Album extends Container {
  private static final long serialVersionUID = -2566717397577163202L;
  @j2jLazyList("http://imeji.org/terms/item")
  private Collection<URI> images = new ArrayList<URI>();

  @Override
  public void setImages(Collection<URI> images) {
    this.images = images;
  }

  @Override
  public Collection<URI> getImages() {
    return images;
  }
}
