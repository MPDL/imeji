package de.mpg.imeji.presentation.session;

import java.util.List;

import de.mpg.imeji.logic.model.Item;

/**
 * SEt of methods to control objects that are stored in the {@link SessionBean}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SessionObjectsController {
  private final SessionBean session;

  /**
   * Default constructor: Initialize the {@link SessionBean}
   */
  public SessionObjectsController() {
    session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  }

  /**
   * Add the item to the {@link List} of selected {@link Item} stored in the {@link SessionBean}.
   *
   * @param itemURI
   */
  public void selectItem(String itemURI) {
    if (!session.getSelected().contains(itemURI.toString())) {
      session.getSelected().add(itemURI.toString());
    }
  }

  /**
   * Remove the item from the {@link List} of selected {@link Item} stored in the
   * {@link SessionBean}
   *
   * @param itemURI
   */
  public void unselectItem(String itemURI) {
    if (session.getSelected().contains(itemURI.toString())) {
      session.getSelected().remove(itemURI.toString());
    }
  }
}
