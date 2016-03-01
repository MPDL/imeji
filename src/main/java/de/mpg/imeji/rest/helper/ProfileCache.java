package de.mpg.imeji.rest.helper;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.rest.api.ProfileService;

/**
 * This class is a cache to read profiles, to avoid to read too often the profiles
 * 
 * @author bastiens
 *
 */
public class ProfileCache {

  private Map<String, MetadataProfile> profiles = new HashMap<>();
  private ProfileService profileService = new ProfileService();
  private static final Logger LOGGER = Logger.getLogger(ProfileCache.class);

  /**
   * If the profile is cached, read it from cache, else, read it from the database and cache it
   * 
   * @param uri
   * @return
   */
  public MetadataProfile read(URI uri) {
    if (uri == null )
        return null;
    if (profiles.containsKey(uri.toString())) {
      return profiles.get(uri.toString());
    }
    MetadataProfile profile = readFromDatabase(uri);
    profiles.put(uri.toString(), profile);
    return profile;
  }

  /**
   * Read the profile from the database
   * 
   * @param uri
   * @return
   */
  private MetadataProfile readFromDatabase(URI uri) {
    try {
      return profileService.read(uri);
    } catch (Exception e) {
      LOGGER.info("Something nasty happend after reading the profile", e);
      return null;
    }
  }


}
