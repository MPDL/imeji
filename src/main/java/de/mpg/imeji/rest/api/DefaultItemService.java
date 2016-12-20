package de.mpg.imeji.rest.api;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.CREATE;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.UPDATE;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FilenameUtils;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemWithFileTO;
import de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory;
import de.mpg.imeji.rest.transfer.TransferObjectFactory;

/**
 * API Service for {@link DefaultItemTO}
 *
 * @author bastiens
 *
 */
public class DefaultItemService implements API<DefaultItemTO> {
  private final ItemService controller = new ItemService();

  @Override
  public DefaultItemTO create(DefaultItemTO to, User u) throws ImejiException {
    if (to instanceof DefaultItemWithFileTO) {
      // get newFilename
      String filename = getFilename((DefaultItemWithFileTO) to);
      // Get Collection
      CollectionImeji collection = getCollection(to.getCollectionId(), u);
      // transfer TO into item
      Item item = new Item();
      ReverseTransferObjectFactory.transferDefaultItem(to, item, u, CREATE);
      item = controller.create(item, collection, ((DefaultItemWithFileTO) to).getFile(), filename,
          u, ((DefaultItemWithFileTO) to).getFetchUrl(),
          ((DefaultItemWithFileTO) to).getReferenceUrl());
      // transfer item into ItemTO
      DefaultItemTO createdTO = new DefaultItemTO();
      TransferObjectFactory.transferDefaultItem(item, createdTO);
      return createdTO;
    } else {
      throw new BadRequestException(
          "A file must be uploaded, referenced or fetched from external location.");
    }
  }

  @Override
  public DefaultItemTO read(String id, User u) throws ImejiException {
    DefaultItemTO defaultTO = new DefaultItemTO();
    Item item = controller.retrieve(ObjectHelper.getURI(Item.class, id), u);
    TransferObjectFactory.transferDefaultItem(item, defaultTO);
    return defaultTO;
  }

  @Override
  public DefaultItemTO update(DefaultItemTO to, User u) throws ImejiException {
    Item item = controller.retrieveLazy(ObjectHelper.getURI(Item.class, to.getId()), u);
    // Get the collection
    CollectionImeji collection = getCollection(ObjectHelper.getId(item.getCollection()), u);
    // Transfer the item
    ReverseTransferObjectFactory.transferDefaultItem(to, item, u, UPDATE);
    if (to instanceof DefaultItemWithFileTO) {
      DefaultItemWithFileTO tof = (DefaultItemWithFileTO) to;
      String url = getExternalFileUrl(tof);
      if (tof.getFile() != null) {
        item = controller.updateFile(item, collection, tof.getFile(), to.getFilename(), u);
      } else if (!StringHelper.isNullOrEmptyTrim(url)) {
        item = controller.updateWithExternalFile(item, collection, getExternalFileUrl(tof),
            to.getFilename(), !isNullOrEmpty(tof.getFetchUrl()), u);
      } else {
        item = controller.update(item, u);
      }
    } else {
      item = controller.update(item, u);
    }
    DefaultItemTO createdTO = new DefaultItemTO();
    TransferObjectFactory.transferDefaultItem(item, createdTO);
    return createdTO;
  }

  @Override
  public boolean delete(String id, User u) throws ImejiException {
    controller.delete(id, u);
    return true;
  }

  @Override
  public DefaultItemTO release(String i, User u) throws ImejiException {
    return null;
  }

  @Override
  public DefaultItemTO withdraw(String i, User u, String discardComment) throws ImejiException {
    return null;
  }

  @Override
  public void share(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public void unshare(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public SearchResultTO<DefaultItemTO> search(String q, int offset, int size, User u)
      throws ImejiException {

    List<DefaultItemTO> tos = new ArrayList<>();
    SearchResult result = SearchFactory.create(SEARCH_IMPLEMENTATIONS.ELASTIC)
        .search(SearchQueryParser.parseStringQuery(q), null, u, null, null, offset, size);
    for (Item vo : controller.retrieveBatch(result.getResults(), -1, 0, u)) {
      DefaultItemTO to = new DefaultItemTO();
      TransferObjectFactory.transferDefaultItem(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<DefaultItemTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }

  /**
   * Return the collection of the item
   * 
   * @param item
   * @param u
   * @return
   * @throws ImejiException
   */
  private CollectionImeji getCollection(String collectionId, User u) throws ImejiException {
    if (!StringHelper.isNullOrEmptyTrim(collectionId)) {
      return new CollectionController()
          .retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, collectionId), u);
    }
    throw new UnprocessableError("Item must be uploaded in a collection");
  }

  /**
   * Find the correct filename if there
   *
   * @param to
   * @return
   **/
  public String getFilename(DefaultItemWithFileTO to) {
    return firstNonNullOrEmtpy(to.getFilename(),
        (to.getFile() != null) ? FilenameUtils.getName(to.getFile().getName()) : "",
        FilenameUtils.getName(to.getFetchUrl()), FilenameUtils.getName(to.getReferenceUrl()));
  }

  /**
   * Return the external Url
   *
   * @param to
   * @return
   */
  private String getExternalFileUrl(DefaultItemWithFileTO to) {
    return firstNonNullOrEmtpy(to.getFetchUrl(), to.getReferenceUrl());
  }

  private String firstNonNullOrEmtpy(String... strs) {
    if (strs == null) {
      return null;
    }
    for (String str : strs) {
      if (str != null && !"".equals(str.trim())) {
        return str;
      }
    }
    return null;
  }
}
