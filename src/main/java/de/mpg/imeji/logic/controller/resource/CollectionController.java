/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller.resource;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.log4j.Logger;

import com.google.common.base.Function;
import com.google.common.collect.Lists;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.ImejiController;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.util.SecurityUtil;
import de.mpg.imeji.logic.validation.impl.CollectionValidator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * CRUD controller for {@link CollectionImeji}, plus search mehtods related to
 * {@link CollectionImeji}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class CollectionController extends ImejiController {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.collectionModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.collectionModel);
  private static final Logger LOGGER = Logger.getLogger(CollectionController.class);
  private Search search =
      SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC);

  public static enum MetadataProfileCreationMethod {
    COPY, REFERENCE, NEW;
  }

  /**
   * Default constructor
   */
  public CollectionController() {
    super();
  }

  /**
   * Creates a new collection. - Add a unique id - Write user properties
   *
   * @param c
   * @param p
   * @param user
   * @param method
   * @return
   * @throws ImejiException
   */
  public CollectionImeji create(CollectionImeji c, MetadataProfile p, User user,
      MetadataProfileCreationMethod method, String spaceId) throws ImejiException {
    isLoggedInUser(user);
    // Validate before creating a profile, in case the collection isn't valid
    new CollectionValidator().validate(c, Method.CREATE);
    ProfileController pc = new ProfileController();
    String metadataProfileName = " (Metadata profile)";
    if (p == null && method == MetadataProfileCreationMethod.NEW) {
      p = new MetadataProfile();
      p.setDescription(c.getMetadata().getDescription());
      p.setTitle(c.getMetadata().getTitle() + metadataProfileName);
      p = pc.create(p, user);
    } else if (p != null && method == MetadataProfileCreationMethod.COPY) {
      p.setTitle(c.getMetadata().getTitle() + metadataProfileName);
      p = pc.create(p.clone(), user);
    } else if (p != null && method == MetadataProfileCreationMethod.REFERENCE
        && !SecurityUtil.staticAuth().administrate(user, p) && p.getStatus() != Status.RELEASED) {
      // User must be allowed to release the profile: otherwise, he might not be allowed to release
      // his collection (if profile is already released, no problems)
      throw new NotAllowedError("Not allowed to reference this profile");
    }

    // Check in controller if Profile is released and is created by user
    if (method.equals(MetadataProfileCreationMethod.REFERENCE) && p != null) {
      if (!(p.getCreatedBy().equals(user.getId()) || Status.RELEASED.equals(p.getStatus()))) {
        throw new UnprocessableError(
            "You can not reference the metadata profile with Id=" + p.getIdString()
                + "The profile you reference must be released or must be created by you!");
      }
    }
    prepareCreate(c, user);
    if (p != null) {
      c.setProfile(p.getId());
    }
    WRITER.create(WriterFacade.toList(c), p, user);
    updateCreatorGrants(user, c.getId().toString());
    // check the space
    if (!isNullOrEmpty(spaceId)) {
      SpaceController sp = new SpaceController();
      sp.addCollection(spaceId, c.getId().toString(), user);
    }
    return c;
  }

  /**
   * Retrieve a complete {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji})
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji retrieve(URI uri, User user) throws ImejiException {
    return (CollectionImeji) READER.read(uri.toString(), user, new CollectionImeji());
  }

  /**
   * Retrieve the {@link CollectionImeji} without its {@link Item}
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public CollectionImeji retrieveLazy(URI uri, User user) throws ImejiException {
    return (CollectionImeji) READER.readLazy(uri.toString(), user, new CollectionImeji());
  }

  /**
   * Load {@link CollectionImeji} defined in a {@link List} of uris. Don't load the {@link Item}
   * contained in the {@link CollectionImeji}
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   * @throws ImejiException
   */
  public Collection<CollectionImeji> retrieveBatchLazy(List<String> uris, int limit, int offset,
      User user) throws ImejiException {
    List<CollectionImeji> cols = prepareBatchRetrieve(uris, limit, offset);
    READER.readLazy(J2JHelper.cast2ObjectList(cols), user);
    return cols;

  }

  /**
   * Prepare the list of {@link Collection} which is going to be retrieved
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   */
  private List<CollectionImeji> prepareBatchRetrieve(List<String> uris, int limit, int offset) {
    List<CollectionImeji> collections = new ArrayList<CollectionImeji>();
    uris = uris.size() > 0 && limit > 0 ? uris.subList(offset, getMin(offset + limit, uris.size()))
        : uris;
    for (String s : uris) {
      collections.add((CollectionImeji) J2JHelper.setId(new CollectionImeji(), URI.create(s)));
    }
    return collections;
  }

  /**
   * Retrieve all {@link CollectionImeji} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public Collection<CollectionImeji> retrieveAll(User user) throws ImejiException {
    List<String> uris =
        ImejiSPARQL.exec(JenaCustomQueries.selectCollectionAll(), Imeji.collectionModel);
    return retrieveBatchLazy(uris, -1, 0, user);
  }

  /**
   * Update a {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji})
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public CollectionImeji update(CollectionImeji ic, User user) throws ImejiException {
    prepareUpdate(ic, user);
    WRITER.update(WriterFacade.toList(ic), null, user, true);
    return retrieve(ic.getId(), user);
  }

  /**
   * Update a {@link CollectionImeji} (inclusive its {@link Item}: slow for huge
   * {@link CollectionImeji}) TODO remove if possible
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public CollectionImeji updateWithProfile(CollectionImeji ic, MetadataProfile mp, User user,
      MetadataProfileCreationMethod method) throws ImejiException {
    updateCollectionProfile(ic, mp, user, method);
    prepareUpdate(ic, user);
    return update(ic, user);
  }

  /**
   * Update a {@link CollectionImeji} (with its Logo)
   *
   * @param ic
   * @param hasgrant
   * @throws ImejiException
   */
  public void updateLogo(CollectionImeji ic, File f, User u)
      throws ImejiException, IOException, URISyntaxException {
    ic = (CollectionImeji) setLogo(ic, f);
    update(ic, u);
  }


  /**
   * Update the {@link CollectionImeji} but not iths {@link Item}
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public CollectionImeji updateLazy(CollectionImeji ic, User user) throws ImejiException {
    prepareUpdate(ic, user);
    WRITER.updateLazy(WriterFacade.toList(ic), null, user);
    return retrieveLazy(ic.getId(), user);
  }

  // TODO Move this method to profilecontroller
  public CollectionImeji updateCollectionProfile(CollectionImeji ic, MetadataProfile mp, User user,
      MetadataProfileCreationMethod method) throws ImejiException {
    if (mp == null) {
      return ic;
    }
    ProfileController pc = new ProfileController();
    if (method.equals(MetadataProfileCreationMethod.REFERENCE)) {
      // if it is a reference, only change the reference to the new
      // metadata profile, and do not forget to delete old metadata
      // profile
      ic.setProfile(mp.getId());
      // Update the collection with the new profile
      ic = update(ic, user);
      // here update of the newly referenced profile for all Items (if
      // there are any, patch is applied)
      updateCollectionItemsProfile(ic, mp.getId(), user);
    } else {
      // Clone the profile
      MetadataProfile copy = mp.clone();
      copy.setTitle(mp.getTitle() + " - COPY");
      // Create the cloned profile
      copy = pc.create(copy, user);
      // Set the cloned profile to the collection
      ic.setProfile(copy.getId());
      // Update the collection with the cloned profile
      ic = update(ic, user);
      // here update of the newly referenced profile for all Items (if
      // there are any, patch is applied)
      updateCollectionItemsProfile(ic, copy.getId(), user);
    }
    return ic;
  }

  /**
   * Update the {@link CollectionImeji} but not iths {@link Item} TODO : remove if possible
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public CollectionImeji updateLazyWithProfile(CollectionImeji ic, MetadataProfile mp, User user,
      MetadataProfileCreationMethod method) throws ImejiException {
    updateCollectionProfile(ic, mp, user, method);
    prepareUpdate(ic, user);
    return updateLazy(ic, user);
  }


  /**
   * Delete a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void delete(CollectionImeji collection, User user) throws ImejiException {
    ItemBusinessController itemController = new ItemBusinessController();
    List<String> itemUris =
        itemController.search(collection.getId(), null, null, user, null, -1, 0).getResults();
    if (hasImageLocked(itemUris, user)) {
      throw new RuntimeException("Collection can not be deleted: It contains locked items:");
    } else {
      if (collection.getStatus() != Status.PENDING && !SecurityUtil.isSysAdmin(user)) {
        throw new UnprocessableError("collection_is_not_pending");
      }
      // Delete images
      List<Item> items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
      for (Item it : items) {
        if (it.getStatus().equals(Status.RELEASED)) {
          throw new UnprocessableError("collection_has_released_items");
        }
      }

      itemController.delete(items, user);

      // Delete profile if it is set for the collection
      if (collection.getProfile() != null) {
        ProfileController pc = new ProfileController();
        try {

          MetadataProfile collectionMdp = pc.retrieve(collection.getProfile(), user);
          if ((pc.isReferencedByOtherResources(collectionMdp.getId().toString(),
              collection.getId().toString()))
              || (!SecurityUtil.staticAuth().delete(user, collectionMdp.getId().toString()))
              || collectionMdp.getDefault()) {
            LOGGER.info(
                "Metadata profile related to this collection is referenced elsewhere, or user does not have permission to delete this profile."
                    + "Profile <" + collectionMdp.getId().toString() + "> will not be deleted!");
          } else {
            if (collectionMdp.getStatus().equals(Status.PENDING)) {
              LOGGER.info("Metadata profile <" + collectionMdp.getId().toString()
                  + "> is not referenced elsewhere, will be deleted!");
              pc.delete(collectionMdp, user, collection.getId().toString());
            } else {
              LOGGER.info("Metadata profile <" + collectionMdp.getId().toString()
                  + "> is not referenced elsewhere, could be deleted, "
                  + "but it is not, as it has status " + collectionMdp.getStatus().name() + "!");
            }
          }
        } catch (NotFoundException e) {
          LOGGER.info("Collection profile does not exist, could not be deleted.");
        }
      }

      WRITER.delete(WriterFacade.toList(collection), user);
    }
  }

  /**
   * Release a {@link CollectionImeji} and all its {@link Item}
   *
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void release(CollectionImeji collection, User user) throws ImejiException {
    ItemBusinessController itemController = new ItemBusinessController();

    isLoggedInUser(user);

    if (collection == null) {
      throw new NotFoundException("collection object does not exists");
    }

    prepareRelease(collection, user);
    List<String> itemUris =
        itemController.search(collection.getId(), null, null, user, null, -1, 0).getResults();

    if (hasImageLocked(itemUris, user)) {
      throw new UnprocessableError("Collection has locked items: can not be released");
    } else if (itemUris.isEmpty()) {
      throw new UnprocessableError("An empty collection can not be released!");
    } else {
      List<Item> items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
      itemController.release(items, user);
      update(collection, user);
      if (collection.getProfile() != null
          && SecurityUtil.staticAuth().administrate(user, collection.getProfile().toString())) {
        ProfileController pc = new ProfileController();
        MetadataProfile profile = pc.retrieve(collection.getProfile(), user);
        if (profile.getStatus() == Status.PENDING) {
          pc.release(profile, user);
        }
      }
    }
  }



  /**
   * Withdraw a {@link CollectionImeji} and all its {@link Item}
   *
   * @param coll
   * @throws ImejiException
   */
  public void withdraw(CollectionImeji coll, User user) throws ImejiException {
    ItemBusinessController itemController = new ItemBusinessController();
    isLoggedInUser(user);

    if (coll == null) {
      throw new NotFoundException("Collection does not exists");
    }

    prepareWithdraw(coll, null);

    List<String> itemUris =
        itemController.search(coll.getId(), null, null, user, null, -1, 0).getResults();
    if (hasImageLocked(itemUris, user)) {
      throw new UnprocessableError("Collection has locked images: can not be withdrawn");
    } else {
      List<Item> items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
      itemController.withdraw(items, coll.getDiscardComment(), user);
      update(coll, user);
      if (coll.getProfile() != null
          && !coll.getProfile().equals(Imeji.defaultMetadataProfile.getId())
          && SecurityUtil.staticAuth().administrate(user, coll.getProfile().toString())) {
        // Withdraw profile
        ProfileController pc = new ProfileController();
        if (!pc.isReferencedByOtherResources(coll.getProfile().toString(),
            coll.getId().toString())) {
          MetadataProfile delMp = pc.retrieve(coll.getProfile(), user);
          delMp.setDiscardComment(coll.getDiscardComment());
          pc.withdraw(delMp, user);
        }
      }
    }
  }

  /**
   * Search for {@link Collection}
   *
   * @param searchQuery
   * @param sortCri
   * @param limit
   * @param offset
   * @return
   */
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, int limit, int offset,
      User user, String spaceId) {
    return search.search(searchQuery, sortCri, user, null, spaceId, offset, limit);
  }

  /**
   * Search and Retrieve Collections
   *
   * @param searchQuery
   * @param sortCri
   * @param limit
   * @param offset
   * @param user
   * @param spaceId
   * @return
   * @throws ImejiException
   */
  public List<CollectionImeji> searchAndRetrieve(SearchQuery searchQuery, SortCriterion sortCri,
      User user, String spaceId, int offset, int size) throws ImejiException {
    SearchResult result = search.search(searchQuery, sortCri, user, null, spaceId, offset, size);
    return (List<CollectionImeji>) retrieveBatchLazy(result.getResults(), -1, 0, user);
  }


  public MetadataProfileCreationMethod getProfileCreationMethod(String method) {
    if ("reference".equalsIgnoreCase(method)) {
      return MetadataProfileCreationMethod.REFERENCE;
    } else if ("copy".equalsIgnoreCase(method)) {
      return MetadataProfileCreationMethod.COPY;
    } else if ("new".equalsIgnoreCase(method)) {
      return MetadataProfileCreationMethod.NEW;
    } else {
      return MetadataProfileCreationMethod.REFERENCE;
    }
  }

  /**
   * Reindex all collections
   * 
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing collections...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.folders, ElasticService.ANALYSER);
    List<CollectionImeji> collections = (List<CollectionImeji>) retrieveAll(Imeji.adminUser);
    indexer.indexBatch(collections);
    LOGGER.info("collections reindexed!");
  }

  private void updateCollectionItemsProfile(CollectionImeji ic, URI newProfileUri, User user)
      throws ImejiException {
    ItemBusinessController itemController = new ItemBusinessController();
    List<String> itemUris =
        itemController.search(ic.getId(), null, null, user, null, -1, 0).getResults();

    List<Item> items = (List<Item>) itemController.retrieveBatch(itemUris, -1, 0, user);
    itemController.updateItemsProfile(items, user, newProfileUri.toString());
  }

  // TODO Remove and replace with normal search method
  public List<CollectionImeji> retrieveCollectionsNotInSpace(final User u) {
    return Lists.transform(
        ImejiSPARQL.exec(JenaCustomQueries.selectCollectionsNotInSpace(), Imeji.collectionModel),
        new Function<String, CollectionImeji>() {
          @Override
          public CollectionImeji apply(String id) {
            try {
              return retrieve(URI.create(id), u);
            } catch (ImejiException e) {
              LOGGER.info("Cannot retrieve collection: " + id);
            }
            return null;
          }
        });
  }

  // TODO Remove and replace with normal search method
  public List<String> retrieveAllCollectionIdsInSpace(URI spaceId) {
    return ImejiSPARQL.exec(JenaCustomQueries.selectCollectionImejiOfSpace(spaceId.toString()),
        Imeji.collectionModel);
  }

  // TODO Remove and replace with method checking the cache, related to ElasticIndexer.java (see
  // comment there as well)
  public String retrieveSpaceOfCollection(URI collectionId) {
    List<String> collectionSpace =
        ImejiSPARQL.exec(JenaCustomQueries.selectSpaceOfCollection(collectionId), null);
    if (collectionSpace.isEmpty()) {
      return null;
    } else {
      return collectionSpace.get(0);
    }
  }
}
