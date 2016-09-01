/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.net.URI;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * If an object is already in a session, return it. <br/>
 * Increase performance compared to {@link ObjectLoader}
 *
 * @author saquet
 */
public class ObjectCachedLoader {
  /**
   * private Constructor
   */
  private ObjectCachedLoader() {}

  /**
   * Load a {@link MetadataProfile} from the session if possible, otherwise from jena
   *
   * @param uri
   * @return
   */
  public static MetadataProfile loadProfile(URI uri) {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    MetadataProfile profile = sessionBean.getProfileCached().get(uri);
    if (profile == null) {
      profile = ObjectLoader.loadProfile(uri, sessionBean.getUser());
      if (profile != null) {
        sessionBean.getProfileCached().put(profile.getId(), profile);
      }
    }
    return profile;
  }

  /**
   * Load a {@link MetadataProfile} from the session if possible, otherwise from jena
   *
   * @param uri
   * @return
   * @throws ImejiException
   */
  public static MetadataProfile loadProfileWithoutPrivs(URI uri) throws ImejiException {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    MetadataProfile profile = sessionBean.getProfileCached().get(uri);
    if (profile == null) {
      profile = new ProfileController().retrieve(uri, Imeji.adminUser);
      if (profile != null) {
        sessionBean.getProfileCached().put(profile.getId(), profile);
      }
    }
    return profile;
  }
}
