package de.mpg.imeji.logic.controller.business;

import java.net.URI;
import java.util.List;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.util.ImejiFactory;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;

/**
 * Business Controller for {@link MetadataProfile}
 *
 * @author bastiens
 *
 */
public class MetadataProfileBusinessController {
  private static final Logger LOGGER = Logger.getLogger(MetadataProfileBusinessController.class);
  private ProfileController metadataProfileController = new ProfileController();
  public static final String DEFAULT_METADATA_PROFILE_PATH_PROPERTY =
      "default-metadata-profile.json";
  private static final String DEFAULT_METADATA_TITLE = "Default Metadata Profile";

  /***
   * Create default profile.
   *
   * @return default metadata profile
   * @throws ImejiException
   **/

  public MetadataProfile initDefaultMetadataProfile() {
    try {
      MetadataProfile mdpVO = retrieveDefaultProfile();
      if (mdpVO == null) {
        Imeji.defaultMetadataProfile=null;
        LOGGER.info("Creating default metadata profile...");
        MetadataProfile defaultProfile = new MetadataProfile();
        defaultProfile.setTitle(DEFAULT_METADATA_TITLE);
        defaultProfile.setDefault(true);
        defaultProfile.getStatements().add(ImejiFactory.newStatement("Title", "en", Types.TEXT));
        defaultProfile.getStatements()
            .add(ImejiFactory.newStatement("Author", "en", Types.CONE_PERSON));
        defaultProfile.getStatements()
            .add(ImejiFactory.newStatement("Date of creation", "en", Types.DATE));
        defaultProfile.getStatements()
            .add(ImejiFactory.newStatement("Geolocation", "en", Types.GEOLOCATION));
        defaultProfile.getStatements()
            .add(ImejiFactory.newStatement("License", "en", Types.LICENSE));
        mdpVO = metadataProfileController.create(defaultProfile, Imeji.adminUser);
        LOGGER.info("...done!");
      }
      if (mdpVO != null && !mdpVO.getStatus().equals(Status.RELEASED)) {
        metadataProfileController.release(mdpVO, Imeji.adminUser);
      }
      return mdpVO;
    } catch (ImejiException e) {
      LOGGER.error("Error creating default profile", e);
    }
    return null;

  }

  /**
   * Find default profile.
   *
   * @return default metadata profile
   * @throws ImejiException
   */

  public MetadataProfile retrieveDefaultProfile() throws ImejiException {
    Search search = SearchFactory.create();
    List<String> uris =
        search.searchString(JenaCustomQueries.selectDefaultMetadataProfile(), null, null, 0, -1)
            .getResults();
    if (uris.size() == 1) {
      return metadataProfileController.retrieve(URI.create(uris.get(0)), Imeji.adminUser);
    } else if (uris.size() > 1) {
      LOGGER.warn("Data inconsistency: " + uris.size() + " + default metadata profile have been found.");
      return metadataProfileController.retrieve(URI.create(uris.get(0)), Imeji.adminUser);
    } else {
      LOGGER.info("Cannot find default metadata profile...");
    }
    return null;
  }

}
