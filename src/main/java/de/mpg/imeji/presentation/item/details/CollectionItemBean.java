package de.mpg.imeji.presentation.item.details;

import java.io.IOException;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Bean for the detail item page when viewed within a collection
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "CollectionItemBean")
@ViewScoped
public class CollectionItemBean extends ItemBean {
  private static final long serialVersionUID = -6273094031705225499L;
  private final String collectionId;
  private static final Logger LOGGER = Logger.getLogger(CollectionItemBean.class);

  public CollectionItemBean() {
    super();
    this.collectionId = UrlHelper.getParameterValue("collectionId");
    this.prettyLink = "pretty:EditImageOfCollection";
  }

  @Override
  protected void initBrowsing() {
    if (getImage() != null) {
      setBrowse(new ItemDetailsBrowse(getImage(), "collection",
          ObjectHelper.getURI(CollectionImeji.class, collectionId).toString(), getSessionUser()));
    }
  }

  @Override
  public void redirectToBrowsePage() {
    try {
      redirect(getNavigation().getCollectionUrl() + collectionId);
    } catch (final IOException e) {
      LOGGER.error("Error redirect to browse page", e);
    }
  }

  @Override
  public String getNavigationString() {
    return "pretty:CollectionItem";
  }

  public String getCollectionId() {
    return collectionId;
  }
}
