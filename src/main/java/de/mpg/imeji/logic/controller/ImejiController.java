/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.AuthenticationError;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.ImejiNamespaces;
import de.mpg.imeji.logic.ImejiTriple;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.storage.Storage.FileResolution;
import de.mpg.imeji.logic.storage.internal.InternalStorageManager;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Container;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.workflow.WorkflowManager;
import de.mpg.j2j.helper.DateHelper;

/**
 * Abstract class for the controller in imeji dealing with imeji VO: {@link Item}
 * {@link CollectionImeji} {@link Album} {@link User} {@link MetadataProfile}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class ImejiController {
  private static final WorkflowManager WORKFLOW_MANAGER = new WorkflowManager();

  public static final String LOGO_STORAGE_SUBDIRECTORY = "/thumbnail";

  /**
   * If a user is not logged in, throw a Exception
   * 
   * @param user
   * @throws AuthenticationError
   */
  protected void isLoggedInUser(User user) throws AuthenticationError {
    if (user == null) {
      throw new AuthenticationError(AuthenticationError.USER_MUST_BE_LOGGED_IN);
    }
  }

  /**
   * Add the {@link Properties} to an imeji object when it is created
   * 
   * @param properties
   * @param user
   * @throws WorkflowException
   */
  protected void prepareCreate(Properties properties, User user) throws WorkflowException {
    WORKFLOW_MANAGER.prepareCreate(properties, user);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is updated
   * 
   * @param properties
   * @param user
   */
  protected void prepareUpdate(Properties properties, User user) {
    WORKFLOW_MANAGER.prepareUpdate(properties, user);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is released
   * 
   * @param properties
   * @param user
   * @throws WorkflowException
   */
  protected void prepareRelease(Properties properties, User user) throws WorkflowException {
    WORKFLOW_MANAGER.prepareRelease(properties);
  }

  /**
   * Add the {@link Properties} to an imeji object when it is withdrawn
   * 
   * @param properties
   * @param comment
   * @throws WorkflowException
   * @throws UnprocessableError
   */
  protected void prepareWithdraw(Properties properties, String comment) throws WorkflowException {
    if (comment != null && !"".equals(comment)) {
      properties.setDiscardComment(comment);
    }
    WORKFLOW_MANAGER.prepareWithdraw(properties);
  }

  /**
   * Get all the triples which need to be updated by a release
   * 
   * @param uri
   * @param securityUri
   * @return
   */
  protected List<ImejiTriple> getUpdateTriples(String uri, User user, Object o) {
    List<ImejiTriple> triples = new ArrayList<ImejiTriple>();
    triples.add(new ImejiTriple(uri, ImejiNamespaces.MODIFIED_BY, user.getId(), o));
    triples.add(new ImejiTriple(uri, ImejiNamespaces.LAST_MODIFICATION_DATE,
        DateHelper.getCurrentDate(), o));
    return triples;
  }

  /**
   * Get all the triples which need to be updated by an update
   * 
   * @param uri
   * @param securityUri
   * @return
   */
  protected List<ImejiTriple> getReleaseTriples(String uri, Object o) {
    List<ImejiTriple> triples = new ArrayList<ImejiTriple>();
    triples.add(new ImejiTriple(uri, ImejiNamespaces.VERSION, 1, o));
    triples.add(new ImejiTriple(uri, ImejiNamespaces.VERSION_DATE, DateHelper.getCurrentDate(), o));
    triples.add(new ImejiTriple(uri, ImejiNamespaces.STATUS, Status.RELEASED.getURI(), o));
    return triples;
  }

  /**
   * Get all the triples which need to be updated by an update
   * 
   * @param uri
   * @param securityUri
   * @return
   */
  protected List<ImejiTriple> getContainerLogoTriples(String uri, Object o, String logoUrl) {
    List<ImejiTriple> triples = new ArrayList<ImejiTriple>();
    triples.add(new ImejiTriple(uri, ImejiNamespaces.CONTAINER_LOGO, URI.create(logoUrl), o));
    return triples;
  }

  /**
   * Get all the triples which need to be updated by an update of a space
   * 
   * @param uri
   * @param securityUri
   * @return
   */
  protected List<ImejiTriple> getContainerSpaceTriples(String uri, Object o, URI spaceId) {
    List<ImejiTriple> triples = new ArrayList<ImejiTriple>();
    URI.create(uri);
    triples.add(new ImejiTriple(uri, ImejiNamespaces.SPACE, URI.create(spaceId.toString()), o));
    return triples;
  }

  /**
   * Get all the triples which need to be updated by an update
   * 
   * @param uri
   * @param securityUri
   * @return
   * @throws UnprocessableError
   */
  protected List<ImejiTriple> getWithdrawTriples(String uri, Object o, String comment)
      throws UnprocessableError {
    List<ImejiTriple> triples = new ArrayList<ImejiTriple>();
    if (comment != null && !"".equals(comment)) {
      triples.add(new ImejiTriple(uri, ImejiNamespaces.DISCARD_COMMENT, comment, o));
    } else {
      throw new UnprocessableError("Discard error: A Discard comment is needed");
    }
    triples.add(new ImejiTriple(uri, ImejiNamespaces.STATUS, Status.WITHDRAWN.getURI(), o));
    return triples;
  }



  /**
   * True if at least one {@link Item} is locked by another {@link User}
   * 
   * @param uris
   * @param user
   * @return
   */
  protected boolean hasImageLocked(List<String> uris, User user) {
    for (String uri : uris) {
      if (Locks.isLocked(uri.toString(), user.getEmail())) {
        return true;
      }
    }
    return false;
  }

  /**
   * Update logo of {@link Container}
   * 
   * @param Container
   * @param f
   * @param user
   * @return
   * @throws ImejiException
   * @throws URISyntaxException
   */
  protected Container updateFile(Container container, File f, User user)
      throws ImejiException, IOException, URISyntaxException {
    InternalStorageManager ism = new InternalStorageManager();
    if (f != null) {
      String url = ism.generateUrl(container.getIdString(), f.getName(), FileResolution.THUMBNAIL);
      container.setLogoUrl(URI.create(url));
      ism.replaceFile(f, url);
    } else {
      ism.removeFile(container.getLogoUrl().toString());
      container.setLogoUrl(null);
    }
    return container;
  }

  public static int getMin(int a, int b) {
    if (a < b) {
      return a;
    }
    return b;
  }

}
