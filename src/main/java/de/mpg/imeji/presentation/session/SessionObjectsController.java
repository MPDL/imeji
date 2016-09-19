package de.mpg.imeji.presentation.session;

import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;

/**
 * SEt of methods to control objects that are stored in the {@link SessionBean}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SessionObjectsController {
  private SessionBean session;

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

  /**
   * Add a list of uri to the active {@link Album} in the session
   *
   * @param uris
   * @throws ImejiException
   * @throws Exception
   */
  public void addToActiveAlbum(List<String> uris) throws ImejiException {
    AlbumController ac = new AlbumController();
    ac.addToAlbum(session.getActiveAlbum(), uris, session.getUser());
    reloadActiveAlbum();
  }

  /**
   * Remove the list of uri from the active {@link Album} in the session
   *
   * @param uris
   * @throws Exception
   */
  public int removeFromActiveAlbum(List<String> uris) throws ImejiException {
    AlbumController ac = new AlbumController();
    try {
      int deleted = ac.removeFromAlbum(session.getActiveAlbum(), uris, session.getUser());
      return deleted;
    } finally {
      reloadActiveAlbum();
    }
  }

  /**
   * Reload active {@link Album} and set in the {@link SessionBean}
   */
  public void reloadActiveAlbum() {
    if (session.getActiveAlbum() != null) {
      ItemBusinessController ic = new ItemBusinessController();
      session.setActiveAlbum((Album) ic.searchAndSetContainerItems(session.getActiveAlbum(),
          session.getUser(), -1, 0));
    }
  }


}
