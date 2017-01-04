package de.mpg.imeji.rest.api;

import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.transferCollection;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.CREATE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.collection.CollectionController;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.transfer.TransferObjectFactory;

/**
 * API Service for {@link CollectionTO}
 *
 * @author bastiens
 *
 */
public class CollectionAPIService implements APIService<CollectionTO> {
  private CollectionTO getCollectionTO(String id, User u) throws ImejiException {
    final CollectionTO to = new CollectionTO();
    TransferObjectFactory.transferCollection(getCollectionVO(id, u), to);
    return to;
  }

  private CollectionImeji getCollectionVO(String id, User u) throws ImejiException {
    return new CollectionController().retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
  }

  @Override
  public CollectionTO read(String id, User u) throws ImejiException {
    return getCollectionTO(id, u);
  }


  /**
   * Read all the items of a collection according to search query. Response is done with the default
   * format
   *
   * @param id
   * @param u
   * @param q
   * @return
   * @throws ImejiException
   * @throws IOException
   */
  public SearchResultTO<DefaultItemTO> readItems(String id, User u, String q, int offset, int size)
      throws ImejiException {
    final List<DefaultItemTO> tos = new ArrayList<>();
    final ItemService controller = new ItemService();
    final SearchResult result = SearchFactory.create(SEARCH_IMPLEMENTATIONS.ELASTIC).search(
        SearchQueryParser.parseStringQuery(q), null, u,
        ObjectHelper.getURI(CollectionImeji.class, id).toString(), null, offset, size);
    for (final Item vo : controller.retrieveBatch(result.getResults(), -1, 0, u)) {
      final DefaultItemTO to = new DefaultItemTO();
      TransferObjectFactory.transferDefaultItem(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<DefaultItemTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }


  @Override
  public CollectionTO create(CollectionTO to, User u) throws ImejiException {
    // toDo: Move to Controller
    final CollectionController cc = new CollectionController();

    final CollectionImeji vo = new CollectionImeji();
    transferCollection(to, vo, CREATE, u);

    URI collectionURI = null;
    collectionURI = cc.create(vo, u, null).getId();
    return read(CommonUtils.extractIDFromURI(collectionURI), u);
  }

  @Override
  public CollectionTO update(CollectionTO to, User u) throws ImejiException {
    final CollectionController cc = new CollectionController();
    final CollectionImeji vo = getCollectionVO(to.getId(), u);
    final CollectionImeji updatedCollection = cc.update(vo, u);
    final CollectionTO newTO = new CollectionTO();
    TransferObjectFactory.transferCollection(updatedCollection, newTO);
    return newTO;
  }

  @Override
  public CollectionTO release(String id, User u) throws ImejiException {
    final CollectionController controller = new CollectionController();
    final CollectionImeji vo =
        controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.release(vo, u, null);
    // Now Read the collection and return it back
    return getCollectionTO(id, u);

  }

  @Override
  public boolean delete(String id, User u) throws ImejiException {
    final CollectionController controller = new CollectionController();
    final CollectionImeji vo =
        controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.delete(vo, u);
    return true;
  }

  @Override
  public CollectionTO withdraw(String id, User u, String discardComment) throws ImejiException {
    final CollectionController controller = new CollectionController();
    final CollectionImeji vo =
        controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    vo.setDiscardComment(discardComment);
    controller.withdraw(vo, u);
    // Now Read the withdrawn collection and return it back
    return getCollectionTO(id, u);
  }

  @Override
  public void share(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public void unshare(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public SearchResultTO<CollectionTO> search(String q, int offset, int size, User u)
      throws ImejiException {
    final CollectionController cc = new CollectionController();
    final List<CollectionTO> tos = new ArrayList<>();
    final SearchResult result =
        SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC)
            .search(SearchQueryParser.parseStringQuery(q), null, u, null, null, offset, size);
    for (final CollectionImeji vo : cc.retrieveBatchLazy(result.getResults(), -1, 0, u)) {
      final CollectionTO to = new CollectionTO();
      TransferObjectFactory.transferCollection(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<CollectionTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }
}
