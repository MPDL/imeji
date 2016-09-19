package de.mpg.imeji.logic.controller.resource;

import static com.google.common.io.Files.copy;
import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.util.PropertyReader;
import de.mpg.imeji.logic.controller.ImejiController;
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
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * CRUD methods for {@link Space}
 *
 * @author vmakarenko (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SpaceController extends ImejiController {
  private static final Logger LOGGER = LoggerFactory.getLogger(SpaceController.class);
  private static final ReaderFacade READER = new ReaderFacade(Imeji.spaceModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.spaceModel);
  public static final String SPACES_STORAGE_SUBDIRECTORY = "/spaces";

  /**
   * The directory path where files are stored
   */
  private final String storagePath;
  /**
   * The URL used to access the storage (this is a dummy url, used by the internal storage to parse
   * file location)
   */
  private String storageUrl = null;

  private final CollectionController cc = new CollectionController();

  public SpaceController() {
    try {

      File storageDir =
          new File(PropertyReader.getProperty("imeji.storage.path") + SPACES_STORAGE_SUBDIRECTORY);
      storagePath = StringHelper.normalizePath(storageDir.getAbsolutePath());
      storageUrl = StringHelper.normalizeURI(PropertyReader.getProperty("imeji.instance.url"))
          + "file" + SPACES_STORAGE_SUBDIRECTORY + StringHelper.urlSeparator;
    } catch (Exception e) {
      throw new RuntimeException("Internal spaces storage couldn't be initialized!!!!!", e);
    }
  }

  /**
   * Creates a new space. - Add a unique id - Write user properties
   *
   * @param space
   * @param user
   */
  public Space create(Space space, User user) throws ImejiException {
    space.setStatus(Properties.Status.RELEASED);
    prepareCreate(space, user);
    // TODO: here is future grants definitions
    /*
     * GrantController gc = new GrantController(); gc.addGrants(user,
     * AuthorizationPredefinedRoles.admin(space.getId() .toString(), null), user);
     */

    WRITER.create(WriterFacade.toList(space), null, user);

    // add collections if exist
    Collection<String> spaceCollections = space.getSpaceCollections();
    if (spaceCollections != null && !spaceCollections.isEmpty()) {
      setSpaceInCollections(space, spaceCollections, user, false);
    }

    return space;
  }

  public Space create(Space space, Collection<String> newSpaceCollections, File file, User user)
      throws ImejiException, IOException {
    if (newSpaceCollections != null && !newSpaceCollections.isEmpty()) {
      space.setSpaceCollections(newSpaceCollections);
    }
    Space newSpace = create(space, user);

    if (file != null && file.exists()) {

      updateFile(newSpace, file, user);

    }

    return newSpace;
  }

  public Space update(Space space, Collection<String> newSpaceCollections, File file, User user)
      throws ImejiException, IOException {

    CollectionController cc = new CollectionController();
    List<String> alreadyAssignedCollections = cc.retrieveAllCollectionIdsInSpace(space.getId());
    List<String> toRemoveCollections = new ArrayList<String>();

    for (String previousColId : alreadyAssignedCollections) {
      if (!(newSpaceCollections.contains(previousColId))) {
        toRemoveCollections.add(previousColId);
      }
    }

    if (newSpaceCollections != null && !newSpaceCollections.isEmpty()) {
      space.setSpaceCollections(newSpaceCollections);
      setSpaceInCollections(space, space.getSpaceCollections(), user, false);
    }

    if (!toRemoveCollections.isEmpty()) {
      setSpaceInCollections(space, toRemoveCollections, user, true);
    }

    if (file != null && file.exists()) {

      updateFile(space, file, user);

    }

    // update space for reindexing at the end
    space.setSpaceCollections(newSpaceCollections);
    Space spaceUpdated = update(space, user);
    return spaceUpdated;
  }

  /**
   * Updates a space -Logged in users: --User is space owner --OR user is space editor
   *
   * @param space
   * @param user
   * @throws ImejiException
   */
  public Space update(Space space, User user) throws ImejiException {
    prepareUpdate(space, user);
    WRITER.update(WriterFacade.toList(space), null, user, true);
    return retrieve(space.getId(), user);
  }

  /**
   * Update logo of {@link Space}
   *
   * @param space
   * @param f
   * @param user
   * @return
   * @throws ImejiException
   */
  public Space updateFile(Space space, File f, User user) throws ImejiException, IOException {
    space.setLogoUrl(URI.create(generateUrl(ObjectHelper.getId(space.getId()), f.getName())));
    update(f, transformUrlToPath(space.getLogoUrl().toURL().toString()));
    return update(space, user);
  }

  /**
   * Copy the file in the file system
   *
   * @param toCopy
   * @param path
   * @return
   * @throws IOException
   */
  private String update(File toCopy, String path) throws IOException {
    File f = new File(path);
    if (f.getParentFile().exists()) {
      // clean space dir
      FileUtils.cleanDirectory(f.getParentFile());
    } else {
      // create space dir
      f.getParentFile().mkdirs();
    }
    copy(toCopy, f);
    return f.getAbsolutePath();
  }

  /**
   * Create the URL of the file from its filename, its id, and its resolution. Important: the
   * filename is decoded, to avoid problems by reading this url
   *
   * @param id
   * @param filename
   * @return
   * @throws UnsupportedEncodingException
   */
  public String generateUrl(String id, String filename) {
    filename = StringHelper.normalizeFilename(filename);
    return storageUrl + id + StringHelper.urlSeparator + filename;
  }

  /**
   * Transform an url to a file system path
   *
   * @param url
   * @return
   */
  public String transformUrlToPath(String url) {
    return URI.create(url).getPath().replace(URI.create(storageUrl).getPath(), storagePath)
        .replace(StringHelper.urlSeparator, StringHelper.fileSeparator);
  }

  /**
   * Transform the path of the item into a path
   *
   * @param path
   * @return
   */
  public String transformPathToUrl(String path) {
    return path.replace(storagePath, storageUrl).replace(StringHelper.fileSeparator,
        StringHelper.urlSeparator);
  }

  /**
   * Remove space file storage
   *
   * @param space
   * @throws IOException
   */
  public void removeFile(Space space) throws IOException {
    if (space == null || space.getLogoUrl() == null) {
      return;
    }
    String url = space.getLogoUrl().toURL().toString();
    if (isNullOrEmptyTrim(url)) {
      return;
    }
    File f = new File(transformUrlToPath(url)).getParentFile();
    if (f.exists()) {
      FileUtils.deleteDirectory(f);
    }
  }

  /**
   * Updates an space -Logged in users: --User is space owner --OR user is space editor
   *
   * @param space
   * @param user
   * @throws ImejiException
   */
  public void updateLazy(Space space, User user) throws ImejiException {
    prepareUpdate(space, user);
    WRITER.updateLazy(WriterFacade.toList(space), null, user);
  }

  /**
   * Retrieve {@link Space}
   *
   * @param spaceId
   * @param user
   * @return
   * @throws ImejiException
   */
  public Space retrieve(URI spaceId, User user) throws ImejiException {
    Space space = (Space) READER.read(spaceId.toString(), user, new Space());
    space.setSpaceCollections(retrieveCollections(space));
    return space;
  }

  /**
   * Retrieve {@link Space}
   *
   * @param space
   * @param user
   * @return
   * @throws ImejiException
   */
  public Space retrieve(Space space, User user) throws ImejiException {
    return retrieve(space.getId(), user);
  }

  /**
   * Retrieve all imeji {@link Album}
   *
   * @return
   * @throws ImejiException
   */
  public List<Space> retrieveAll() throws ImejiException {
    return Lists.transform(ImejiSPARQL.exec(JenaCustomQueries.selectSpaceAll(), Imeji.spaceModel),
        new Function<String, Space>() {
          @Override
          public Space apply(String id) {
            try {
              return retrieve(URI.create(id), Imeji.adminUser);
            } catch (ImejiException e) {
              LOGGER.info("Cannot retrieve space: " + id);
            }
            return null;
          }
        });
  }

  /**
   * Retrieve all {@link CollectionImeji}s of {@link Space}
   *
   * @param space
   * @return
   * @throws ImejiException
   * @throws URISyntaxException
   */
  public Collection<String> retrieveCollections(Space space, boolean force) throws ImejiException {
    List<String> currentSpaceCollections = new ArrayList<>();
    if (force || Iterables.isEmpty(space.getSpaceCollections())) {
      for (String colUri : ImejiSPARQL
          .exec(JenaCustomQueries.selectCollectionsOfSpace(space.getId()), null)) {
        currentSpaceCollections.add(colUri);
      }
      space.setSpaceCollections(currentSpaceCollections);
    }
    return space.getSpaceCollections();
  }

  public Collection<String> retrieveCollections(Space space) throws ImejiException {
    return retrieveCollections(space, false);
  }

  /**
   * Add {@link CollectionImeji} to {@link Space}
   *
   * @param space
   * @param collId
   * @param user
   * @throws ImejiException
   */
  public void addCollection(Space space, String collId, User user) throws ImejiException {
    if (!Iterables.contains(space.getSpaceCollections(), collId)) {
      space.getSpaceCollections().add(collId);
      // update(space, user);
      setSpaceInCollections(space, Lists.newArrayList(collId), user, false);
    }
  }

  /**
   * Add {@link CollectionImeji} to {@link Space}
   *
   * @param space
   * @param toAdd
   * @param user
   * @throws ImejiException
   */
  public void addCollections(Space space, List<String> toAdd, User user) throws ImejiException {

    space.setSpaceCollections(retrieveCollections(space));
    Collection<String> spaceCollections = space.getSpaceCollections();
    Iterables.removeAll(toAdd, spaceCollections);
    if (!toAdd.isEmpty()) {
      for (String collId : toAdd) {
        spaceCollections.add(collId);
      }
      space.setSpaceCollections(spaceCollections);
      // update(space, user);
      setSpaceInCollections(space, spaceCollections, user, false);
    }

  }

  /**
   * Add {@link CollectionImeji} to {@link Space}
   *
   * @param spaceId
   * @param collId
   * @param user
   * @throws ImejiException
   */
  public void addCollection(String spaceId, String collId, User user) throws ImejiException {
    Space sp = retrieve(URI.create(spaceId), user);
    addCollection(sp, collId, user);
  }

  /**
   *
   * Remove {@link CollectionImeji} from the {@link Space}
   *
   * @param space
   * @param collId
   * @param user
   * @return renewed list
   * @throws ImejiException
   */
  public Collection<String> removeCollection(Space space, final String collId, User user)
      throws ImejiException {
    return removeCollections(space, Lists.newArrayList(collId), user);
  }

  /**
   *
   * Remove {@link CollectionImeji} from the {@link Space}
   *
   * @param space
   * @param collsToRemove
   * @param user
   * @return renewed list
   * @throws ImejiException
   */
  public Collection<String> removeCollections(Space space, Collection<String> collsToRemove,
      User user) throws ImejiException {
    Collection<String> colls = retrieveCollections(space, true);
    Iterables.removeAll(colls, collsToRemove);

    space.setSpaceCollections(colls);
    setSpaceInCollections(space, collsToRemove, user, true);

    return colls;

  }


  private void setSpaceInCollections(Space space, Collection<String> collIds, User user,
      boolean remove) throws ImejiException {
    for (String collId : collIds) {
      // NB comment: seems the whole collIdURI comes from input
      CollectionImeji c = cc.retrieve(URI.create(collId), user);
      c.setSpace(remove ? null : space.getId());
      try {
        cc.update(c, user);
      } catch (Exception e) {
        throw new UnprocessableError(
            "Error during patching collection " + c.getId() + " with space Id " + c.getSpace());
      }
    }
  }

  /**
   * Retrieve an {@link Space} without its {@link Item}
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public Space retrieveLazy(URI uri, User user) throws ImejiException {
    return (Space) READER.readLazy(uri.toString(), user, new Space());
  }

  /**
   * Search for {@link Space}
   *
   * @param searchQuery
   * @param sortCri
   * @param limit
   * @param offset
   * @return
   */
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, int limit, int offset,
      User user, String spaceId) {
    Search search = SearchFactory.create(SearchObjectTypes.SPACE, SEARCH_IMPLEMENTATIONS.ELASTIC);
    return search.search(searchQuery, sortCri, user, null, null, 0, -1);
  }

  public Space retrieveSpaceByLabel(String spaceId, User user) throws ImejiException {
    Search s = SearchFactory.create();
    List<String> r =
        s.searchString(JenaCustomQueries.getSpaceByLabel(spaceId), null, null, 0, -1).getResults();
    if (!r.isEmpty() && !isNullOrEmptyTrim(r.get(0))) {
      return retrieve(URI.create(r.get(0)), user);
    } else {
      return null;
    }
  }

  /**
   * Delete the {@link Space}
   *
   * @param space
   * @param user
   * @throws ImejiException
   */
  public void delete(Space space, User user) throws ImejiException, IOException {
    removeFile(space);
    removeCollections(space, retrieveCollections(space, true), user);
    WRITER.delete(WriterFacade.toList(space), user);
  }

  /**
   * Reindex all spaces
   * 
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing Spaces...");
    ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.spaces, ElasticService.ANALYSER);
    List<Space> items = retrieveAll();
    LOGGER.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    LOGGER.info("Spaces reindexed!");
  }

  public boolean isSpaceByLabel(String spaceId) {
    if (isNullOrEmptyTrim(spaceId)) {
      return false;
    }
    if (ImejiSPARQL.exec(JenaCustomQueries.getSpaceByLabel(spaceId), Imeji.spaceModel).size() > 0) {
      return true;
    }
    return false;
  }

  /**
   * @param url
   * @return Checks if the URL provided is URL for Space Logo
   */
  public boolean isSpaceLogoURL(String url) {
    return url.contains(Imeji.FILE_SERVLET_PATH + SPACES_STORAGE_SUBDIRECTORY);
  }

}
