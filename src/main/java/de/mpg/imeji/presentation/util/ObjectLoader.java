/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.util;

import java.net.URI;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;

/**
 * imeji objects (item, collection, album, profile) loader. This loader should be used to loads
 * objects from a Java bean, since it include error message. Doesn't use caching (unlike
 * {@link ObjectCachedLoader})
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ObjectLoader {
  private static final Logger LOGGER = Logger.getLogger(ObjectLoader.class);

  /**
   * Private Constructor
   */
  private ObjectLoader() {}



  /**
   * Load a {@link MetadataProfile}
   *
   * @param id
   * @param user
   * @return
   */
  public static MetadataProfile loadProfile(URI id, User user) {
    try {
      ProfileController pc = new ProfileController();
      MetadataProfile p = pc.retrieve(id, user);
      return p;
    } catch (Exception e) {
      LOGGER.info("There was a problem loading the profile with id " + id.toString());
    }
    return null;
  }

}
