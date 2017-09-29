package de.mpg.imeji.logic.core.item;

import static com.google.common.base.Strings.isNullOrEmpty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import com.google.common.base.Predicate;
import com.google.common.collect.Collections2;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.QuotaExceededException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.content.ContentService;
import de.mpg.imeji.logic.generic.SearchServiceAbstract;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ContentVO;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.License;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.model.factory.ItemFactory;
import de.mpg.imeji.logic.model.util.LicenseUtil;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.elasticsearch.ElasticIndexer;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService;
import de.mpg.imeji.logic.search.elasticsearch.ElasticService.ElasticTypes;
import de.mpg.imeji.logic.search.facet.model.Facet;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.jenasearch.ImejiSPARQL;
import de.mpg.imeji.logic.search.jenasearch.JenaCustomQueries;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.security.user.UserService;
import de.mpg.imeji.logic.security.user.util.QuotaUtil;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;

/**
 * Implements CRUD and Search methods for {@link Item}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ItemService extends SearchServiceAbstract<Item> {
  private static final Logger LOGGER = Logger.getLogger(ItemService.class);
  public static final String NO_THUMBNAIL_URL = "NO_THUMBNAIL_URL";
  private final Search search =
      SearchFactory.create(SearchObjectTypes.ITEM, SEARCH_IMPLEMENTATIONS.ELASTIC);
  private final ItemController itemController = new ItemController();

  /**
   * Controller constructor
   */
  public ItemService() {
    super(SearchObjectTypes.ITEM);
  }

  /**
   * Create an {@link Item} in a {@link CollectionImeji}
   *
   * @param item
   * @param coll
   * @param user
   * @throws ImejiException
   * @return
   */
  public Item create(Item item, CollectionImeji coll, User user) throws ImejiException {
    create(Arrays.asList(item), coll, user);
    return item;
  }

  /**
   * Create an {@link Item} for a {@link File}.
   *
   * @param f
   * @param filename (optional)
   * @param c - the collection in which the file is uploaded
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item createWithFile(Item item, File f, String filename, CollectionImeji c, User user)
      throws ImejiException {
    if (item == null) {
      item = ImejiFactory.newItem(c);
    }
    preValidateUpload(filename, c, f, user);
    item.setFilename(filename);
    item.setFileSize(f.length());
    item.setFiletype(StorageUtils.getMimeType(f));
    item = create(item, c, user);
    new ContentService().create(item, f, user);
    return item;
  }

  /**
   * Check if the file ca be uploaded
   * 
   * @param filename
   * @param c
   * @param f
   * @param user
   * @throws ImejiException
   */
  private void preValidateUpload(String filename, CollectionImeji c, File f, User user)
      throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(filename)) {
      throw new UnprocessableError("Filename must not be empty!");
    }
    validateChecksum(c.getId(), f, false);
    validateFileFormat(f);
    QuotaUtil.checkQuota(user, f, c);
  }

  /**
   * Upload a File to the staging area. Will not be created as Item.
   * 
   * @param uploadId
   * @param f
   * @param filename
   * @param c
   * @param user
   * @throws ImejiException
   */
  public void uploadToStaging(String uploadId, File f, String filename, CollectionImeji c,
      User user) throws ImejiException {
    StagingService service = new StagingService();
    validateFileFormat(f);
    long quota = service.getUsedQuota(uploadId, user);
    QuotaUtil.checkQuota(quota, user, f, c);
    String checksum = StorageUtils.calculateChecksum(f);
    service.validateChecksum(uploadId, checksum);
    validateChecksum(checksum, c.getId(), f, false);
    service.add(uploadId, new ItemFactory().setCollection(c.getId().toString())
        .setFilename(filename).setFile(f).build(), f, quota);
  }


  /**
   * Create an {@link Item} with an external {@link File} according to its URL
   *
   * @param item
   * @param c
   * @param externalFileUrl
   * @param download
   * @param user
   * @return
   * @throws IOException
   * @throws ImejiException
   */
  public Item createWithExternalFile(Item item, CollectionImeji c, String externalFileUrl,
      String filename, boolean download, User user) throws ImejiException {
    final String origName = FilenameUtils.getName(externalFileUrl);
    if ("".equals(filename) || filename == null) {
      filename = origName;
    }
    // Filename extension will be added if not provided.
    // Will not be appended if it is the same value from original external reference again
    // Original external reference will be appended to the provided extension in addition
    else if (FilenameUtils.getExtension(filename).equals("")
        || !FilenameUtils.getExtension(filename).equals(FilenameUtils.getExtension(origName))) {
      filename = filename + "." + FilenameUtils.getExtension(origName);
    }

    if (filename == null || filename.equals("")) {
      throw new BadRequestException(
          "Could not derive the filename. Please provide the filename with the request!");
    }

    if (externalFileUrl == null || externalFileUrl.equals("")) {
      throw new BadRequestException("Please provide fetchUrl or referenceUrl with the request!");
    }
    if (item == null) {
      item = ImejiFactory.newItem(c);
    }
    if (download) {
      // download the file in storage
      final File tmp = readFile(externalFileUrl);
      item = createWithFile(item, tmp, filename, c, user);
    } else {
      item.setFilename(filename);
      item.setFiletype(StorageUtils.getMimeType(FilenameUtils.getExtension(externalFileUrl)));
      item = create(item, c, user);
      new ContentService().create(item, externalFileUrl, user);
    }
    return item;
  }

  /**
   * Create a {@link List} of {@link Item} in a {@link CollectionImeji}. This method is faster than
   * using create(Item item, URI coll) when creating many items
   *
   * @param items
   * @param coll
   * @throws ImejiException
   */
  public void create(Collection<Item> items, CollectionImeji col, User user) throws ImejiException {
    if (col == null || col.getId() == null) {
      throw new UnprocessableError("Collection and Collection id have to be non-null");
    }
    items.stream().forEach(item -> {
      item.setStatus(col.getStatus());
      item.setCollection(col.getId());
    });
    itemController.createBatch((List<Item>) items, user);
  }

  /**
   * Create an {@link Item}
   *
   * @param item
   * @param uploadedFile
   * @param filename
   * @param u
   * @param fetchUrl
   * @param referenceUrl
   * @return
   * @throws ImejiException
   */
  public Item create(Item item, CollectionImeji collection, File uploadedFile, String filename,
      User u, String fetchUrl, String referenceUrl) throws ImejiException {
    isLoggedInUser(u);
    if (uploadedFile != null && isNullOrEmpty(filename)) {
      throw new BadRequestException("Filename for the uploaded file must not be empty!");
    }

    if (uploadedFile == null && isNullOrEmpty(fetchUrl) && isNullOrEmpty(referenceUrl)) {
      throw new BadRequestException(
          "Please upload a file or provide one of the fetchUrl or referenceUrl as input.");
    }
    Item newItem = new Item(item);
    if (uploadedFile != null) {
      newItem = createWithFile(item, uploadedFile, filename, collection, u);
    } else if (getExternalFileUrl(fetchUrl, referenceUrl) != null) {
      // If no file, but either a fetchUrl or a referenceUrl
      newItem = createWithExternalFile(item, collection, getExternalFileUrl(fetchUrl, referenceUrl),
          filename, downloadFile(fetchUrl), u);
    } else {
      throw new BadRequestException("Filename or reference must not be empty!");
    }

    return newItem;
  }

  /**
   * User ObjectLoader to load image
   *
   * @param imgUri
   * @return
   * @throws ImejiException
   */
  public Item retrieve(URI imgUri, User user) throws ImejiException {
    return itemController.retrieve(imgUri.toString(), user);
  }

  public Item retrieve(String imgUri, User user) throws ImejiException {
    return itemController.retrieve(imgUri, user);
  }

  public Item retrieveLazy(URI imgUri, User user) throws ImejiException {
    return itemController.retrieveLazy(imgUri.toString(), user);
  }

  public Item retrieveLazy(String imgUri, User user) throws ImejiException {
    return itemController.retrieveLazy(imgUri, user);
  }

  /**
   * Lazy Retrieve the Item containing the file with the passed storageid
   *
   * @param storageId
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item retrieveLazyForFile(String fileUrl, User user) throws ImejiException {
    final Search s = SearchFactory.create(SearchObjectTypes.ALL, SEARCH_IMPLEMENTATIONS.JENA);
    final List<String> r =
        s.searchString(JenaCustomQueries.selectItemOfFile(fileUrl), null, null, 0, -1).getResults();
    if (!r.isEmpty() && r.get(0) != null) {
      return retrieveLazy(URI.create(r.get(0)), user);
    } else {
      throw new NotFoundException("Can not find the resource requested");
    }
  }

  /**
   * Retrieve the items lazy (without the metadata)
   *
   * @param uris
   * @param limit
   * @param offset
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveBatch(List<String> uris, int limit, int offset, User user)
      throws ImejiException {
    return itemController.retrieveBatch(uris, user);
  }

  /**
   * Retrieve the items fully (with all metadata)
   *
   * @param uris
   * @param limit
   * @param offset
   * @param user
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveBatchLazy(List<String> uris, int limit, int offset, User user)
      throws ImejiException {
    return itemController.retrieveBatchLazy(uris, user);
  }


  /**
   * Retrieve all {@link Item} (all status, all users) in imeji
   *
   * @return
   * @throws ImejiException
   */
  public Collection<Item> retrieveAll(User user) throws ImejiException {
    final List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectItemAll(), Imeji.imageModel);
    LOGGER.info(uris.size() + " items found, retrieving...");
    return retrieveBatch(uris, -1, 0, user);
  }

  /**
   * Update an {@link Item} in the database
   *
   * @param item
   * @param user
   * @throws ImejiException
   */
  public Item update(Item item, User user) throws ImejiException {
    updateBatch(Arrays.asList(item), user);
    return retrieve(item.getId(), user);
  }

  /**
   * Update a {@link Collection} of {@link Item}
   *
   * @param items
   * @param user
   * @throws ImejiException
   */
  public void updateBatch(Collection<Item> items, User user) throws ImejiException {
    itemController.updateBatch((List<Item>) items, user);
  }

  /**
   * Update the File of an {@link Item}
   *
   * @param item
   * @param f
   * @param user
   * @return
   * @throws ImejiException
   */
  public Item updateFile(Item item, CollectionImeji col, File f, String filename, User user)
      throws ImejiException {
    validateChecksum(item.getCollection(), f, true);
    preValidateUpload(filename, col, f, user);
    if (filename != null) {
      item.setFilename(filename);
    }
    item.setFileSize(f.length());
    item.setFiletype(StorageUtils.getMimeType(f));
    item = update(item, user);
    new ContentService().update(item, f, user);
    return item;
  }

  /**
   * Update the {@link Item} with External link to File.
   *
   * @param item
   * @param externalFileUrl
   * @param filename
   * @param download
   * @param u
   * @return
   * @throws ImejiException
   */
  public Item updateWithExternalFile(Item item, CollectionImeji col, String externalFileUrl,
      String filename, boolean download, User u) throws ImejiException {
    final String origName = FilenameUtils.getName(externalFileUrl);
    filename =
        isNullOrEmpty(filename) ? origName : filename + "." + FilenameUtils.getExtension(origName);
    item.setFilename(filename);
    if (download) {
      final File tmp = readFile(externalFileUrl);
      item = updateFile(item, col, tmp, filename, u);
    } else {
      if (filename != null) {
        item.setFilename(filename);
      }
      item = update(item, u);
      new ContentService().update(item, externalFileUrl, u);
    }
    return item;

  }

  /**
   * Delete a {@link List} of {@link Item} inclusive all files stored in the {@link Storage}
   *
   * @param items
   * @param user
   * @return
   * @throws ImejiException
   */
  public void delete(List<Item> items, User user) throws ImejiException {
    itemController.deleteBatch(items, user);
    for (final Item item : items) {
      removeFileFromStorage(item);
    }
  }

  /**
   * Delete a {@link List} of {@link Item} inclusive all files stored in the {@link Storage}
   *
   * @param itemId
   * @param u
   * @return
   * @throws ImejiException
   */
  public void delete(String itemId, User u) throws ImejiException {
    final Item item = retrieve(ObjectHelper.getURI(Item.class, itemId), u);
    delete(Arrays.asList(item), u);
  }

  /**
   * Search {@link Item}
   *
   * @param containerUri - if the search is done within a {@link Container}
   * @param searchQuery - the {@link SearchQuery}
   * @param sortCri - the {@link SortCriterion}
   * @param user
   * @param size
   * @param offset
   * @return
   */
  public SearchResult search(URI containerUri, SearchQuery searchQuery, SortCriterion sortCri,
      User user, int size, int offset) {
    return search.search(searchQuery, sortCri, user,
        containerUri != null ? containerUri.toString() : null, offset, size);
  }

  /**
   * Search and add {@link Facet} to the {@link SearchResult}
   * 
   * @param containerUri
   * @param searchQuery
   * @param sortCri
   * @param user
   * @param size
   * @param offset
   * @return
   */
  public SearchResult searchWithFacets(URI containerUri, SearchQuery searchQuery,
      SortCriterion sortCri, User user, int size, int offset) {
    return search.searchWithFacets(searchQuery, sortCri, user,
        containerUri != null ? containerUri.toString() : null, offset, size);
  }

  /**
   * load items of a container. Perform a search to load all items: is faster than to read the
   * complete container
   *
   * @param c
   * @param user
   */
  public CollectionImeji searchAndSetContainerItems(CollectionImeji c, User user, int limit,
      int offset) {
    final List<String> newUris = search(c.getId(), null, null, user, limit, 0).getResults();
    c.getImages().clear();
    for (final String s : newUris) {
      c.getImages().add(URI.create(s));
    }
    return c;
  }

  /**
   * Retrieve all items filtered by query
   *
   * @param user
   * @param q
   * @return
   * @throws ImejiException
   */
  public List<Item> searchAndRetrieve(URI containerUri, SearchQuery q, SortCriterion sort,
      User user, int offset, int size) throws ImejiException, IOException {
    List<Item> itemList = new ArrayList<Item>();
    try {
      final List<String> results = search(containerUri, q, sort, user, size, offset).getResults();
      itemList = (List<Item>) retrieveBatch(results, -1, 0, user);
    } catch (final Exception e) {
      throw new UnprocessableError("Cannot retrieve items:", e);
    }
    return itemList;
  }

  /**
   * Set the status of a {@link List} of {@link Item} to released
   *
   * @param l
   * @param user
   * @param defaultLicens
   * @throws ImejiException
   */
  public void release(List<Item> l, User user, License defaultLicense) throws ImejiException {
    final Collection<Item> items = filterItemsByStatus(l, Status.PENDING);
    for (final Item item : items) {
      prepareRelease(item, user);
      if (defaultLicense != null && LicenseUtil.getActiveLicense(item) == null) {
        item.setLicenses(Arrays.asList(defaultLicense.clone()));
      }
    }
    updateBatch(items, user);
  }

  /**
   * Release the items with the current default license
   *
   * @param l
   * @param user
   * @throws ImejiException
   */
  public void releaseWithDefaultLicense(List<Item> l, User user) throws ImejiException {
    this.release(l, user, itemController.getDefaultLicense());
  }

  /**
   * Set the status of a {@link List} of {@link Item} to withdraw and delete its files from the
   * {@link Storage}
   *
   * @param l
   * @param comment
   * @throws ImejiException
   */
  public void withdraw(List<Item> l, String comment, User user) throws ImejiException {
    final Collection<Item> items = filterItemsByStatus(l, Status.RELEASED);
    for (final Item item : items) {
      prepareWithdraw(item, comment);
    }
    updateBatch(items, user);
    for (final Item item : items) {
      removeFileFromStorage(item);
    }
  }

  /**
   * Reindex all items
   *
   * @param index
   * @throws ImejiException
   */
  public void reindex(String index) throws ImejiException {
    LOGGER.info("Indexing Items...");
    final ElasticIndexer indexer =
        new ElasticIndexer(index, ElasticTypes.items, ElasticService.ANALYSER);
    LOGGER.info("Retrieving Items...");
    final List<Item> items = (List<Item>) retrieveAll(Imeji.adminUser);
    LOGGER.info("+++ " + items.size() + " items to index +++");
    indexer.indexBatch(items);
    LOGGER.info("Items reindexed!");
  }

  /**
   * Copy Items to another collection and remove the original items
   * 
   * @param items
   * @param collectionid
   * @param user
   * @throws ImejiException
   */
  public List<Item> moveItems(List<String> ids, CollectionImeji col, User user, License license)
      throws ImejiException {
    List<Item> items = retrieve(ids, user);
    List<ContentVO> contents = retrieveContentBatchLazy(items);
    contents = filterContentsIfChecksumAlreadyExists(contents, col);
    List<ContentVO> moved = new ContentService().move(items, contents, col.getIdString());
    if (moved.size() > 0) {
      Set<String> movedSet = moved.stream().map(c -> c.getItemId()).collect(Collectors.toSet());
      items = items.stream().filter(item -> movedSet.contains(item.getId().toString()))
          .filter(item -> item.getStatus().equals(Status.PENDING))
          .peek(item -> prepareMoveItem(item, col, license)).collect(Collectors.toList());
      updateBatch(items, user);
      return items;
    }
    return new ArrayList<>();
  }

  /**
   * Prepare an item for move
   * 
   * @param item
   * @param col
   * @param license
   * @return
   */
  private Item prepareMoveItem(Item item, CollectionImeji col, License license) {
    item.setCollection(col.getId());
    item.setStatus(col.getStatus());
    if (license != null && !license.isEmtpy() && LicenseUtil.getActiveLicense(item) == null) {
      item.setLicenses(Arrays.asList(license.clone()));
    }
    return item;
  }

  /**
   * Retrieve the contents of the Items
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  private List<ContentVO> retrieveContentBatchLazy(List<Item> items) throws ImejiException {
    ContentService contentService = new ContentService();
    List<String> contentIds =
        items.stream().map(item -> contentService.findContentId(item.getId().toString()))
            .collect(Collectors.toList());
    return contentService.retrieveBatchLazy(contentIds);
  }

  /**
   * Filter the contents if their checksum exists already in the passed collection
   * 
   * @param items
   * @param col
   * @return
   */
  private List<ContentVO> filterContentsIfChecksumAlreadyExists(List<ContentVO> contents,
      CollectionImeji col) {
    return contents.stream()
        .filter(content -> !checksumExistsInCollection(col.getId(), content.getChecksum()))
        .collect(Collectors.toList());
  }

  /**
   * Return a new filtered List of only item with the requested {@link Status}
   *
   * @param items
   * @param status
   * @return
   */
  private Collection<Item> filterItemsByStatus(List<Item> items, Status status) {
    return new ArrayList<>(Collections2.filter(items, new Predicate<Item>() {
      @Override
      public boolean apply(Item item) {
        return item.getStatus() == status;
      }
    }));
  }

  /**
   * Remove a file from the current {@link Storage}
   *
   * @param id
   */
  private void removeFileFromStorage(Item item) {
    final ContentService contentService = new ContentService();
    try {
      contentService.delete(contentService.findContentId(item.getId().toString()));
    } catch (final Exception e) {
      LOGGER.error("error deleting file", e);
    }
  }


  /**
   * Return the external Url of the File
   *
   * @param fetchUrl
   * @param referenceUrl
   * @return
   */
  private String getExternalFileUrl(String fetchUrl, String referenceUrl) {
    return firstNonNullOrEmtpy(fetchUrl, referenceUrl);
  }

  private String firstNonNullOrEmtpy(String... strs) {
    if (strs == null) {
      return null;
    }
    for (final String str : strs) {
      if (str != null && !"".equals(str.trim())) {
        return str;
      }
    }
    return null;
  }

  /**
   * True if the file must be download in imeji (i.e fetchurl is defined)
   *
   * @param fetchUrl
   * @return
   */
  private boolean downloadFile(String fetchUrl) {
    return !isNullOrEmpty(fetchUrl);
  }


  /**
   * Throws an {@link Exception} if the file cannot be uploaded. The validation will only occur when
   * the file has been stored locally)
   *
   * @throws ImejiException
   * @throws UnprocessableError
   */
  private void validateChecksum(URI collectionURI, File file, Boolean isUpdate)
      throws UnprocessableError, ImejiException {
    validateChecksum(StorageUtils.calculateChecksum(file), collectionURI, file, isUpdate);
  }

  /**
   * Throws an {@link Exception} if the file cannot be uploaded. The validation will only occur when
   * the file has been stored locally)
   * 
   * @param checksum
   * @param collectionURI
   * @param file
   * @param isUpdate
   * @throws UnprocessableError
   * @throws ImejiException
   */
  private void validateChecksum(String checksum, URI collectionURI, File file, Boolean isUpdate)
      throws UnprocessableError, ImejiException {
    if (checksumExistsInCollection(collectionURI, checksum)) {
      throw new UnprocessableError((!isUpdate)
          ? "Same file already exists in the collection (with same checksum). Please choose another file."
          : "Same file already exists in the collection or you are trying to upload same file for the item (with same checksum). Please choose another file.");
    }
  }

  private void validateFileFormat(File file) throws UnprocessableError {
    final StorageController sc = new StorageController();
    final String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
    if (StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat)) {
      throw new UnprocessableError("upload_format_not_allowed " + file.getName());
    }
  }

  /**
   * True if the checksum already exists within another {@link Item} in this {@link CollectionImeji}
   *
   * @param filename
   * @return
   */
  private boolean checksumExistsInCollection(URI collectionId, String checksum) {
    final SearchQuery q = SearchQuery.toSearchQuery(
        new SearchPair(SearchFields.checksum, SearchOperators.EQUALS, checksum, false));
    return search.search(q, null, Imeji.adminUser, collectionId.toString(), 0, 1)
        .getNumberOfRecords() > 0;
  }


  /**
   * Read a file from its url
   *
   * @param tmp
   * @param url
   * @return
   * @throws UnprocessableError
   */
  private File readFile(String url) throws UnprocessableError {
    try {
      final StorageController sController = new StorageController("external");
      final File tmp = TempFileUtil.createTempFile("createOrUploadWithExternalFile", null);
      sController.read(url, new FileOutputStream(tmp), true);
      return tmp;
    } catch (final Exception e) {
      throw new UnprocessableError(e.getLocalizedMessage());
    }
  }

  @Override
  public SearchResult search(SearchQuery searchQuery, SortCriterion sortCri, User user, int size,
      int offset) {
    return search(null, searchQuery, sortCri, user, size, offset);
  }

  @Override
  public List<Item> retrieve(List<String> ids, User user) throws ImejiException {
    return itemController.retrieveBatch(ids, user);
  }

  @Override
  public List<Item> retrieveAll() throws ImejiException {
    final List<String> uris = ImejiSPARQL.exec(JenaCustomQueries.selectItemAll(), Imeji.imageModel);
    LOGGER.info(uris.size() + " items found, retrieving...");
    return (List<Item>) retrieveBatch(uris, -1, 0, Imeji.adminUser);
  }

  /**
   * Check user disk space quota. Quota is calculated for user of target collection.
   *
   * @param file
   * @param col
   * @throws ImejiException
   * @return remained disk space after successfully uploaded <code>file</code>; <code>-1</code> will
   *         be returned for unlimited quota
   */
  public static long checkQuota(User user, File file, CollectionImeji col) throws ImejiException {
    final User targetCollectionUser = col == null || user.getId().equals(col.getCreatedBy()) ? user
        : new UserService().retrieve(col.getCreatedBy(), Imeji.adminUser);

    final Search search = SearchFactory.create();
    final List<String> results =
        search.searchString(JenaCustomQueries.selectUserFileSize(user.getId().toString()), null,
            null, 0, -1).getResults();
    long currentDiskUsage = 0L;
    try {
      currentDiskUsage = Long.parseLong(results.get(0).toString());
    } catch (final NumberFormatException e) {
      throw new UnprocessableError("Cannot parse currentDiskSpaceUsage " + results.get(0).toString()
          + "; requested by user: " + user.getEmail() + "; targetCollectionUser: "
          + targetCollectionUser.getEmail(), e);
    }
    final long needed = currentDiskUsage + file.length();
    if (needed > targetCollectionUser.getQuota()) {
      throw new QuotaExceededException("Data quota ("
          + QuotaUtil.getQuotaHumanReadable(targetCollectionUser.getQuota(), Locale.ENGLISH)
          + " allowed) has been exceeded (" + FileUtils.byteCountToDisplaySize(currentDiskUsage)
          + " used)");
    }
    return targetCollectionUser.getQuota() - needed;
  }
}
