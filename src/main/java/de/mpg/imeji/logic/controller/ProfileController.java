/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.logic.util.ResourceHelper.getStringFromPath;
import static de.mpg.imeji.rest.process.ReverseTransferObjectFactory.transferMetadataProfile;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Logger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.ImejiSPARQL;
import de.mpg.imeji.logic.jobs.CleanMetadataJob;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchFactory;
import de.mpg.imeji.logic.search.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.jenasearch.JenaSearch;
import de.mpg.imeji.logic.search.model.SearchIndex;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.search.model.SortCriterion.SortOrder;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;
import de.mpg.imeji.rest.process.RestProcessUtils;
import de.mpg.imeji.rest.process.ReverseTransferObjectFactory.TRANSFER_MODE;
import de.mpg.imeji.rest.to.MetadataProfileTO;
import de.mpg.j2j.helper.DateHelper;
import de.mpg.j2j.helper.J2JHelper;

/**
 * Controller for {@link MetadataProfile}
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ProfileController extends ImejiController {
  private static final ReaderFacade reader = new ReaderFacade(Imeji.profileModel);
  private static final WriterFacade writer = new WriterFacade(Imeji.profileModel);
  public static final String DEFAULT_METADATA_PROFILE_PATH_PROPERTY =
      "default-metadata-profile.json";
  private static final Logger LOGGER = Logger.getLogger(ProfileController.class);

  /**
   * Default Constructor
   */
  public ProfileController() {
    super();
  }

  /**
   * Create a new Profile.
   * 
   * @param p
   * @param user
   * @return
   * @throws ImejiException
   */
  public MetadataProfile create(MetadataProfile p, User user) throws ImejiException {
    prepareCreate(p, user);
    p.setStatus(Status.PENDING);
    writer.create(WriterFacade.toList(p), null, user);
    ShareController shareController = new ShareController();
    shareController.shareToCreator(user, p.getId().toString());
    return p;
  }

  /**
   * Retrieve a {@link User} by its id
   * 
   * @param id
   * @param user
   * @return
   * @throws ImejiException
   */
  public MetadataProfile retrieve(String id, User user) throws ImejiException {
    return retrieve(ObjectHelper.getURI(MetadataProfile.class, id), user);
  }

  /**
   * Retrieve a {@link User} by its {@link URI}
   * 
   * @param uri
   * @param user
   * @return
   * @throws NotFoundException
   * @throws ImejiException
   */
  public MetadataProfile retrieve(URI uri, User user) throws ImejiException {
    MetadataProfile p = null;
    if (uri == null) {
      return null;
    }
    p = ((MetadataProfile) reader.read(uri.toString(), user, new MetadataProfile()));
    Collections.sort((List<Statement>) p.getStatements());
    return p;
  }

  /**
   * Retrieve a {@link User} by its {@link URI}
   * 
   * @param collectionId
   * @param user
   * @return
   * @throws NotFoundException
   * @throws ImejiException
   */
  public MetadataProfile retrieveByCollectionId(URI collectionId, User user) throws ImejiException {
    CollectionController cc = new CollectionController();
    CollectionImeji c;
    try {
      c = cc.retrieve(collectionId, user);
      if (c.getProfile() == null) {
        return null;
      }
      return retrieve(c.getProfile(), user);
    } catch (NotFoundException e) {
      throw new UnprocessableError("Invalid collection: " + e.getLocalizedMessage());
    }
  }


  /**
   * Retrieve the {@link MetadataProfile} of an {@link Item} TODO: Use Search for better performance
   * 
   * @param itemId
   * @param user
   * @return
   * @throws ImejiException
   */
  public MetadataProfile retrieveByItemId(URI itemId, User user) throws ImejiException {
    try {
      Item item = new ItemController().retrieve(itemId, user);
      CollectionImeji c = new CollectionController().retrieve(item.getCollection(), user);
      return retrieve(c.getProfile(), user);
    } catch (NotFoundException e) {
      throw new UnprocessableError("Invalid collection: " + e.getLocalizedMessage());
    }
  }

  /**
   * Updates a collection -Logged in users: --User is collection owner --OR user is collection
   * editor
   * 
   * @param mdp
   * @param user
   * @throws ImejiException
   */
  public void update(MetadataProfile mdp, User user) throws ImejiException {
    isLoggedInUser(user);
    prepareUpdate(mdp, user);
    writer.update(WriterFacade.toList(mdp), null, user, true);
    Imeji.executor.submit(new CleanMetadataJob(mdp));
  }

  /**
   * Release a {@link MetadataProfile}
   * 
   * @param mdp
   * @param user
   * @throws ImejiException
   */
  public void release(MetadataProfile mdp, User user) throws ImejiException {
    prepareRelease(mdp, user);
    update(mdp, user);
  }

  /**
   * Release a {@link MetadataProfile}
   * 
   * @param id
   * @param user
   * @throws ImejiException
   */
  public void release(String id, User user) throws ImejiException {
    release(retrieve(id, user), user);
  }

  /**
   * Delete a {@link MetadataProfile} from within a collection
   * 
   * @param mdp
   * @param user
   * @throws ImejiException
   */
  public void delete(MetadataProfile mdp, User user, String collectionId) throws ImejiException {
    // First check if there are empty metadata records
    if ((isNullOrEmpty(collectionId) && isReferencedByAnyResources(mdp.getId().toString()))
        || !isNullOrEmpty(collectionId)
            && isReferencedByOtherResources(mdp.getId().toString(), collectionId)) {
      throw new UnprocessableError("error_profile_is_referenced_cannot_be_deleted");
    } else if (mdp.getDefault()) {
      throw new UnprocessableError("error_profile_is_default_cannot_be_deleted");
    }
    writer.delete(WriterFacade.toList(mdp), user);
    Imeji.executor.submit(new CleanMetadataJob(mdp));
  }

  /**
   * Delete a {@link MetadataProfile} , checks if there are any references in other collections
   * before deletion
   * 
   * @param mdp
   * @param user
   * @throws ImejiException
   */
  public void delete(MetadataProfile mdp, User user) throws ImejiException {
    this.delete(mdp, user, "");
  }

  /**
   * Withdraw a {@link MetadataProfile}
   * 
   * @param mdp
   * @param user
   * @throws ImejiException
   */
  public void withdraw(MetadataProfile mdp, User user) throws ImejiException {
    if (mdp.getDefault()) {
      throw new UnprocessableError("error_profile_is_default_cannot_be_withdrawn");
    }
    mdp.setStatus(Status.WITHDRAWN);
    mdp.setVersionDate(DateHelper.getCurrentDate());
    update(mdp, user);
  }

  /**
   * Search for a profile
   * 
   * @param query
   * @param user
   * @return
   */
  public SearchResult search(SearchQuery query, User user, String spaceId) {
    Search search = SearchFactory.create(SearchObjectTypes.PROFILE, SEARCH_IMPLEMENTATIONS.JENA);
    // Automatically sort by profile title
    SortCriterion sortCri = new SortCriterion(
        JenaSearch.getIndex(SearchIndex.SearchFields.prof.name()), SortOrder.ASCENDING);
    SearchResult result = search.search(query, sortCri, user, null, spaceId, 0, -1);
    return result;
  }

  /**
   * Search all profile allowed for the current user. Sorted by profile name, query parameter
   * possible.
   * 
   * @return
   * @throws ImejiException
   */
  public List<MetadataProfile> search(User user, String q, String spaceId) throws ImejiException {
    try {
      SearchResult result = search(SearchQueryParser.parseStringQuery(q), user, spaceId);
      return (List<MetadataProfile>) retrieveLazy(result.getResults(),
          getMin(result.getResults().size(), 500), 0, user);
    } catch (Exception e) {
      LOGGER.error("Cannot retrieve profiles:", e);
    }
    return null;
  }

  /**
   * Search all profile allowed for the current user, Sorted by profile name, no query parameter
   * 
   * @return
   * @throws ImejiException
   */
  public List<MetadataProfile> search(User user, String spaceId) throws ImejiException {
    return search(user, "", spaceId);
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
      return retrieve(URI.create(uris.get(0)), Imeji.adminUser);
    } else if (uris.size() > 1) {
      throw new ImejiException(
          "Data inconsistency: " + uris.size() + " + default metadata profile have been found.");
    } else {
      LOGGER.info("Cannot find default metadata profile...");
    }
    return null;
  }

  /**
   * Create default profile.
   * 
   * @return default metadata profile
   * @throws ImejiException
   */

  public MetadataProfile initDefaultMetadataProfile() throws ImejiException {
    MetadataProfile mdpVO = retrieveDefaultProfile();

    if (mdpVO == null) {
      String path = null;
      String profileJSON = null;
      MetadataProfileTO mdpTO = null;
      try {
        path = new File(this.getClass().getClassLoader()
            .getResource(DEFAULT_METADATA_PROFILE_PATH_PROPERTY).toURI()).getAbsolutePath();
        if (isNullOrEmpty(path)) {
          LOGGER.info(
              "There is no default metadata profile defined! This is not an error, Imeji will still work. Default metadata profile is a convenience for quick start!"
                  + "Check more about it at the IMEJI Documentation.");
          return null;
        }
        profileJSON = getStringFromPath(path);
        mdpTO = (MetadataProfileTO) RestProcessUtils.buildTOFromJSON(profileJSON,
            MetadataProfileTO.class);
      } catch (UnrecognizedPropertyException e) {
        throw new ImejiException(
            "Error reading property " + DEFAULT_METADATA_PROFILE_PATH_PROPERTY + ": " + e);
      } catch (JsonProcessingException e) {
        throw new ImejiException("Cannot process json: " + e);
      } catch (IOException | URISyntaxException e) {
        throw new ImejiException("Wrong path: " + path, e);
      }
      mdpVO = new MetadataProfile();
      transferMetadataProfile(mdpTO, mdpVO, TRANSFER_MODE.CREATE);
      mdpVO.setDefault(true);

      mdpVO = create(mdpVO, Imeji.adminUser);
    }

    if (mdpVO != null && !mdpVO.getStatus().equals(Status.RELEASED)) {
      release(mdpVO, Imeji.adminUser);
    }

    return mdpVO;

  }

  /**
   * Remove all the {@link Metadata} not having a {@link Statement}. This happens when a
   * {@link Statement} has been removed from a {@link MetadataProfile}.
   */
  public void removeMetadataWithoutStatement(MetadataProfile p) {
    String id = p != null ? p.getId().toString() : null;
    ImejiSPARQL.execUpdate(JenaCustomQueries.updateRemoveAllMetadataWithoutStatement(id));
    ImejiSPARQL.execUpdate(JenaCustomQueries.updateEmptyMetadata());
  }

  public boolean isReferencedByOtherResources(String profileUri, String resourceUri) {
    Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    List<String> r =
        s.searchString(JenaCustomQueries.hasOtherMetadataProfileReferences(profileUri, resourceUri),
            null, null, 0, -1).getResults();
    if (r.size() > 0) {
      return true;
    }
    return false;
  }

  public boolean isReferencedByAnyResources(String profileUri) {
    Search s = new JenaSearch(SearchObjectTypes.ALL, null);
    List<String> r = s
        .searchString(JenaCustomQueries.hasMetadataProfileReferences(profileUri), null, null, 0, -1)
        .getResults();
    if (r.size() > 0) {
      return true;
    }
    return false;
  }

  /*
   * 
   * /** Load {@link MetadataProfile} defined in a {@link List} of uris. Don't load the {@link Item}
   * contained in the {@link MetadataProfile}
   * 
   * @param uris
   * 
   * @param limit
   * 
   * @param offset
   * 
   * @return
   * 
   * @throws ImejiException
   */
  public Collection<MetadataProfile> retrieveLazy(List<String> uris, int limit, int offset,
      User user) throws ImejiException {

    List<MetadataProfile> cols = new ArrayList<MetadataProfile>();

    List<String> retrieveUris;
    if (limit < 0) {
      retrieveUris = uris;
    } else {
      retrieveUris = uris.size() > 0 && limit > 0
          ? uris.subList(offset, getMin(offset + limit, uris.size())) : new ArrayList<String>();
    }

    for (String s : retrieveUris) {
      cols.add((MetadataProfile) J2JHelper.setId(new MetadataProfile(), URI.create(s)));
    }

    try {
      reader.readLazy(J2JHelper.cast2ObjectList(cols), user);
      return cols;
    } catch (ImejiException e) {
      LOGGER.error("Error loading metadataProfiles: " + e.getMessage(), e);
      return null;
    }
  }

  public MetadataProfile retrieveLazy(URI imgUri, User user) throws ImejiException {
    return (MetadataProfile) reader.readLazy(imgUri.toString(), user, new MetadataProfile());

  }
}
