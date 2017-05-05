package de.mpg.imeji.logic.item;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.j2j.helper.J2JHelper;
import de.mpg.imeji.logic.collection.CollectionService;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.db.reader.ReaderFacade;
import de.mpg.imeji.logic.db.writer.WriterFacade;
import de.mpg.imeji.logic.service.ImejiControllerAbstract;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.statement.StatementUtil;
import de.mpg.imeji.logic.validation.impl.MetadataValidator;
import de.mpg.imeji.logic.validation.impl.Validator.Method;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.factory.ImejiFactory;
import de.mpg.imeji.logic.vo.util.LicenseUtil;
import de.mpg.imeji.logic.vo.util.MetadataUtil;

/**
 * Resource controller for item
 *
 * @author saquet
 *
 */
class ItemController extends ImejiControllerAbstract<Item> {
  private static final Logger LOGGER = Logger.getLogger(ItemService.class);
  private final ReaderFacade READER;
  private final WriterFacade WRITER;

  public ItemController() {
    READER = new ReaderFacade(Imeji.imageModel);
    WRITER = new WriterFacade(Imeji.imageModel);
  }

  @Override
  public Item create(Item item, User user) throws ImejiException {
    createBatch(Arrays.asList(item), user);
    return item;
  }

  @Override
  public List<Item> createBatch(List<Item> l, User user) throws ImejiException {
    Set<String> collectionIds = new HashSet<>();
    for (final Item item : l) {
      if (!collectionIds.contains(item.getCollection().toString())) {
        // check if the collection exists.
        new CollectionService().retrieve(item.getCollection(), user);
        collectionIds.add(item.getCollection().toString());
      }
      prepareCreate(item, user);
      item.setFilename(FilenameUtils.getName(item.getFilename()));
    }
    cleanItem(l);
    createMissingStatement(l);
    validateMetadata(l, Method.CREATE);
    WRITER.create(J2JHelper.cast2ObjectList(l), user);
    return null;
  }

  @Override
  public Item retrieve(String id, User user) throws ImejiException {
    return (Item) READER.read(id, user, new Item());
  }

  @Override
  public Item retrieveLazy(String id, User user) throws ImejiException {
    return (Item) READER.readLazy(id, user, new Item());
  }

  @Override
  public List<Item> retrieveBatch(List<String> ids, User user) throws ImejiException {
    final List<Item> items = initializeEmptyItems(ids);
    READER.read(J2JHelper.cast2ObjectList(items), user);
    return items;
  }

  @Override
  public List<Item> retrieveBatchLazy(List<String> ids, User user) throws ImejiException {
    final List<Item> items = initializeEmptyItems(ids);
    READER.readLazy(J2JHelper.cast2ObjectList(items), user);
    return items;
  }

  @Override
  public List<Item> updateBatch(List<Item> l, User user) throws ImejiException {
    if (l != null && !l.isEmpty()) {
      for (final Item item : l) {
        prepareUpdate(item, user);
        item.setFilename(FilenameUtils.getName(item.getFilename()));
      }
      cleanItem(l);
      createMissingStatement(l);
      validateMetadata(l, Method.UPDATE);
      WRITER.update(J2JHelper.cast2ObjectList(l), user, true);
    }
    return l;
  }

  /**
   * Update without any validation and any operations on the data. WARNING: use with care. Invalid
   * data would overwrite valid data
   *
   * @param items
   * @param user
   * @throws ImejiException
   */
  public void updateBatchForce(Collection<Item> items, User user) throws ImejiException {
    cleanItem(items);
    WRITER.updateWithoutValidation(new ArrayList<>(items), user);
  }

  @Override
  public void deleteBatch(List<Item> l, User user) throws ImejiException {
    WRITER.delete(new ArrayList<Object>(l), user);
  }

  /**
   * Clean the values of all {@link Metadata} of an {@link Item}
   *
   * @param l
   * @throws ImejiException
   */
  private void cleanItem(Collection<Item> l) {
    for (final Item item : l) {
      if (item.getMetadata() != null) {
        final List<Metadata> cleanMetadata =
            item.getMetadata().stream().sequential().map(md -> MetadataUtil.cleanMetadata(md))
                .filter(md -> !MetadataUtil.isEmpty(md)).collect(Collectors.toList());
        for (Metadata md : item.getMetadata()) {
          md = MetadataUtil.cleanMetadata(md);
        }
        item.setMetadata(cleanMetadata);
      }
      cleanLicenses(item);
    }

  }

  /**
   * Clean the licenses of the item
   *
   * @param item
   * @throws ImejiException
   */
  private void cleanLicenses(Item item) {
    if (item.getLicenses() == null) {
      return;
    }
    final long start = System.currentTimeMillis();
    item.setLicenses(LicenseUtil.removeDuplicates(item.getLicenses()));
    final License active = LicenseUtil.getActiveLicense(item);
    if (active != null && !active.isEmtpy() && active.getStart() < 0) {
      active.setStart(start);
      if (item.getStatus().equals(Status.PENDING)) {
        item.setLicenses(Arrays.asList(active));
      }
      setLicensesEnd(item, active, start);
    } else if ((active == null || active.isEmtpy()) && item.getStatus().equals(Status.PENDING)) {
      item.setLicenses(new ArrayList<>());
    }
  }

  /**
   * Set the end of the licenses (normally, only one license shouldn't have any end)
   *
   * @param item
   * @param current
   * @param end
   */
  private void setLicensesEnd(Item item, License current, long end) {
    for (final License lic : item.getLicenses()) {
      if (lic.getEnd() < 0 && !lic.getName().equals(current.getName())) {
        lic.setEnd(end);
      }
    }
  }

  /**
   * Initialized a list of empty item
   *
   * @param ids
   * @return
   */
  private List<Item> initializeEmptyItems(List<String> ids) {
    return ids.stream().map(id -> ImejiFactory.newItem().setId(id).build())
        .collect(Collectors.toList());
  }

  /**
   * Get all statement of all metadata of all items and create the statement which don't exists
   * already
   * 
   * @param items
   * @throws ImejiException
   */
  private void createMissingStatement(List<Item> items) throws ImejiException {
    List<Statement> l = items.stream().flatMap(i -> i.getMetadata().stream())
        .map(md -> ImejiFactory.newStatement().initFromMetadata(md).build())
        .collect(Collectors.toList());
    StatementUtil.statementListToMap(l).values();
    new StatementService().createBatchIfNotExists(
        new ArrayList<>(StatementUtil.statementListToMap(l).values()), Imeji.adminUser);
  }

  /**
   * Validate the Metadata of the item
   * 
   * @param items
   * @param m
   * @throws ImejiException
   */
  private void validateMetadata(List<Item> items, Method m) throws ImejiException {
    MetadataValidator metadataValidator = new MetadataValidator();
    Map<String, Statement> statementMap =
        StatementUtil.statementListToMap(retrieveStatements(items));
    UnprocessableError error = new UnprocessableError();
    for (Metadata md : items.stream().flatMap(item -> item.getMetadata().stream())
        .collect(Collectors.toList())) {
      try {
        metadataValidator.validate(md, statementMap.get(md.getIndex()), m);
      } catch (UnprocessableError e) {
        error = new UnprocessableError(e.getMessages(), error);
      }
    }
    if (error.hasMessages()) {
      throw error;
    }
  }


  /**
   * Retrieve all statements used by these items
   * 
   * @param items
   * @return
   * @throws ImejiException
   */
  private List<Statement> retrieveStatements(List<Item> items) throws ImejiException {
    return new StatementService().retrieveBatchOnlyExistingStatemment(extractStatementIds(items),
        Imeji.adminUser);
    // return new StatementService().retrieveBatch(extractStatementIds(items), Imeji.adminUser);
  }

  /**
   * Extract all statement uri used by these items
   * 
   * @param items
   * @return
   */
  private List<String> extractStatementIds(List<Item> items) {
    return items.stream().flatMap(item -> item.getMetadata().stream())
        .map(md -> StatementUtil.toUri(md.getIndex())).collect(Collectors.toList());
  }
}
