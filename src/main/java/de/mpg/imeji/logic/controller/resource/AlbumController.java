/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.controller.resource;

import static de.mpg.imeji.logic.util.StringHelper.isNullOrEmptyTrim;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.NotSupportedMethodException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.collaboration.share.ShareBusinessController;
import de.mpg.imeji.logic.reader.ReaderFacade;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.writer.WriterFacade;

/**
 * Implements CRUD and Search methods for {@link Album}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class AlbumController extends ImejiController {
  private static final ReaderFacade READER = new ReaderFacade(Imeji.albumModel);
  private static final WriterFacade WRITER = new WriterFacade(Imeji.albumModel);
  private Search search =
      SearchFactory.create(SearchObjectTypes.ALBUM, SEARCH_IMPLEMENTATIONS.ELASTIC);

  /**
   * Construct a new controller for {@link Album}
   */
  public AlbumController() {
    super();
  }

  /**
   * Creates a new album. - Add a unique id - Write user properties
   *
   * @param album
   * @param user
   */
  public Album create(Album album, User user) throws ImejiException {
    if (!Imeji.CONFIG.getAlbumsEnabled()) {
      throw new NotSupportedMethodException("Album functionalities are disabled");
    }
    isLoggedInUser(user);
    prepareCreate(album, user);
    ShareBusinessController shareController = new ShareBusinessController();
    shareController.shareToCreator(user, album.getId().toString());
    WRITER.create(WriterFacade.toList(album), null, user);
    return album;
  }


  /**
   * Load {@link Album} and {@link Item}: can lead to performance issues
   *
   * @param selectedAlbumId
   * @param user
   * @return
   * @throws ImejiException
   */
  public Album retrieve(URI albumUri, User user) throws ImejiException {
    return retrieveBatch(Arrays.asList(albumUri.toString()), user, 0, -1).get(0);
  }

  /**
   * Retrieve an {@link Album} without its {@link Item}
   *
   * @param uri
   * @param user
   * @return
   * @throws ImejiException
   */
  public Album retrieveLazy(URI albumUri, User user) throws ImejiException {
    return retrieveBatchLazy(Arrays.asList(albumUri.toString()), user, -1, 0).get(0);
  }

  /**
   * Batch retrieve for {@link Album}. Album are lazy retrieved (i.e without {@link Item} list)
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   * @throws ImejiException
   */
  public List<Album> retrieveBatchLazy(List<String> uris, User user, int limit, int offset)
      throws ImejiException {
    List<Album> albums = prepareBatchRetrieve(uris, limit, offset);
    READER.readLazy(J2JHelper.cast2ObjectList(albums), user);
    return albums;
  }

  /**
   * Batch retrieve for {@link Album}. Album are fully retrieved (i.e with {@link Item} list)
   *
   * @param uris
   * @param user
   * @param limit
   * @param offset
   * @return
   * @throws ImejiException
   */
  public List<Album> retrieveBatch(List<String> uris, User user, int limit, int offset)
      throws ImejiException {
    List<Album> albums = retrieveBatchLazy(uris, user, limit, offset);
    ItemController itemController = new ItemController();
    for (Album album : albums) {
      itemController.searchAndSetContainerItems(album, user, -1, 0);
    }
    return albums;
  }

  /**
   * Updates an album -Logged in users: --User is album owner --OR user is album editor, always
   * checking the security
   *
   * @param ic
   * @param user
   * @throws ImejiException
   */
  public Album update(Album album, User user) throws ImejiException {
    prepareUpdate(album, user);
    WRITER.update(WriterFacade.toList(album), null, user, true);
    return retrieve(album.getId(), user);
  }


  /**
   * Delete the {@link Album}
   *
   * @param album
   * @param user
   * @throws ImejiException
   */
  public void delete(Album album, User user) throws ImejiException {
    WRITER.delete(WriterFacade.toList(album), user);
  }

  /**
   * Release and {@link Album}. If one {@link Item} of the {@link Album} is not released, then
   * abort.
   *
   * @param album
   * @throws ImejiException
   */
  public void release(Album album, User user) throws ImejiException {
    prepareRelease(album, user);
    ItemController ic = new ItemController();
    album = (Album) ic.searchAndSetContainerItems(album, user, -1, 0);
    if (album.getImages().isEmpty()) {
      throw new UnprocessableError("An empty album can not be released!");
    } else {
      update(album, user);
    }
  }

  /**
   * Withdraw an {@link Album}: Set the {@link Status} as withdraw and remove all {@link Item}
   *
   * @param album
   * @throws ImejiException
   */
  public void withdraw(Album album, User user) throws ImejiException {
    isLoggedInUser(user);
    if (album == null) {
      throw new NotFoundException("Album does not exists");
    }
    prepareWithdraw(album, album.getDiscardComment());
    album.getImages().clear();
    update(album, user);
  }

  /**
   * Add a list of {@link Item} (as a {@link List} of {@link URI}) to an {@link Album}. Return
   * {@link List} of {@link URI} {@link Item} of the album.
   *
   * @param album
   * @param uris
   * @param user
   * @return
   * @throws ImejiException
   */
  public List<String> addToAlbum(Album album, List<String> uris, User user) throws ImejiException {
    // Check if allowed
    if (Status.WITHDRAWN.equals(album.getStatus())) {
      throw new UnprocessableError("error_album_withdrawn_members_can_not_be_added");
    }
    if (!AuthUtil.staticAuth().create(user, album)) {
      throw new NotAllowedError("album_not_allowed_to_add_item");
    }
    ItemController itemController = new ItemController();
    // Get the item of the album
    List<String> albumItems =
        itemController.search(album.getId(), null, null, Imeji.adminUser, null, -1, 0).getResults();
    // Add Items which are not already in the album
    Set<String> albumItemsSet = new HashSet<>(albumItems);
    // Retrieve the uris, to check that the items all exist
    itemController.retrieveBatch(uris, -1, 0, user);
    for (String uri : uris) {
      albumItemsSet.add(uri);
    }
    album.setImages(new ArrayList<URI>());
    for (String uri : albumItemsSet) {
      album.getImages().add(URI.create(uri));
    }

    // save the album
    update(album, user);
    // Update the new items, to add the relation item -> album in the index
    // We do not update Items of the Album!!!
    // itemController.updateBatch(items, Imeji.adminUser);
    // return all items of the album
    return itemController.search(album.getId(), null, null, Imeji.adminUser, null, -1, 0)
        .getResults();
  }

  /**
   * Remove a list of {@link Item} (as a {@link List} of {@link URI}) to an {@link Album}
   *
   * @param album
   * @param toDelete
   * @param user
   * @return
   * @throws ImejiException
   */
  public int removeFromAlbum(Album album, List<String> toDelete, User user) throws ImejiException {
    ItemController itemController = new ItemController();
    // Get the item of the album
    List<String> albumItems =
        itemController.search(album.getId(), null, null, Imeji.adminUser, null, -1, 0).getResults();
    int beforeSize = albumItems.size();
    // Retrieving Items to check if there will be some not existing item
    itemController.retrieveBatch(toDelete, -1, 0, user);
    for (String uri : toDelete) {
      albumItems.remove(uri);
    }
    album.setImages(new ArrayList<URI>());
    for (String uri : albumItems) {
      album.getImages().add(URI.create(uri));
    }
    if (album.getStatus() == Status.RELEASED && album.getImages().isEmpty()) {
      throw new UnprocessableError("A released album ca not be empty");
    }
    // save the album
    update(album, user);
    // Update the removed items, to remove the relation item -> album in the index
    // We do not update items of the album
    // itemController.updateBatch(items, Imeji.adminUser);
    // Get the new size of the album
    int afterSize = itemController.search(album.getId(), null, null, Imeji.adminUser, null, -1, 0)
        .getNumberOfRecords();
    // Return how many items have been deleted
    return beforeSize - afterSize;
  }

  /**
   * Search for albums - Logged-out user: --Collection must be released -Logged-in users
   * --Collection is released --OR Collection is pending AND user is owner --OR Collection is
   * withdrawn AND user is owner --OR Collection is pending AND user has grant "Container Editor"
   * for it.
   *
   * @param user
   * @param scList
   * @return
   */
  public SearchResult search(SearchQuery searchQuery, User user, SortCriterion sortCri, int size,
      int offset, String spaceId) {
    return search.search(searchQuery, sortCri, user, null, spaceId, offset, size);
  }

  /**
   * Retrieve albums filtered by query
   *
   * @param user
   * @param q
   * @return
   * @throws ImejiException
   */
  public List<Album> searchAndretrieveLazy(User user, String q, String spaceId, int offset,
      int size) throws ImejiException {
    try {
      List<String> results =
          search(!isNullOrEmptyTrim(q) ? SearchQueryParser.parseStringQuery(q) : null, user, null,
              size, offset, spaceId).getResults();
      return retrieveBatchLazy(results, user, -1, 0);
    } catch (Exception e) {
      throw new UnprocessableError("Cannot retrieve albums:", e);
    }
  }


  /**
   * Prepare the list of {@link Album} which is going to be retrieved
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   */
  private List<Album> prepareBatchRetrieve(List<String> uris, int limit, int offset) {
    List<Album> albums = new ArrayList<Album>();
    uris = uris.size() > 0 && limit > 0 ? uris.subList(offset, getMin(offset + limit, uris.size()))
        : uris;
    for (String s : uris) {
      albums.add((Album) J2JHelper.setId(new Album(), URI.create(s)));
    }
    return albums;
  }

  /**
   * Retrieve all {@link Album}. Albums a fully loaded. Might last long
   *
   * @return
   * @throws ImejiException
   */
  public List<Album> retrieveAll(User user) throws ImejiException {
    List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectAlbumAll(), Imeji.albumModel);
    List<Album> albums = prepareBatchRetrieve(uris, -1, 0);
    READER.read(J2JHelper.cast2ObjectList(albums), user);
    return albums;
  }

  /**
   * Update a {@link Album} (with its Logo)
   *
   * @param ic
   * @param hasgrant
   * @throws ImejiException
   */
  public void updateLogo(Album album, File f, User u)
      throws ImejiException, IOException, URISyntaxException {
    album = (Album) setLogo(album, f);
    update(album, u);
  }


  /**
   * Remove a all items from an Album
   *
   * @param album
   * @param toDelete
   * @param user
   * @return
   * @throws ImejiException
   */
  public int removeAllFromAlbum(Album album, User user) throws ImejiException {
    if (album.getStatus() == Status.RELEASED) {
      throw new UnprocessableError("A released album can not be empty! ");
    }
    int beforeSize = album.getImages().size();
    album.setImages(new ArrayList<URI>());
    // save the album
    update(album, user);
    // Return how many items have been removed from album
    return beforeSize;
  }

}
