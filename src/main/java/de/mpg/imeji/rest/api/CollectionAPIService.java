package de.mpg.imeji.rest.api;

import static de.mpg.imeji.rest.transfer.TransferTOtoVO.transferCollection;
import static de.mpg.imeji.rest.transfer.TransferTOtoVO.TRANSFER_MODE.CREATE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.facade.SearchAndRetrieveFacade;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionElement;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.model.factory.ImejiFactory;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.model.SortCriterion;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.to.CollectionElementTO;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.transfer.TransferTOtoVO.TRANSFER_MODE;
import de.mpg.imeji.rest.transfer.TransferVOtoTO;

/**
 * API Service for {@link CollectionTO}
 *
 * @author bastiens
 *
 */
public class CollectionAPIService implements APIService<CollectionTO> {
  
	private CollectionTO getCollectionTO(String id, User u) throws ImejiException {
    final CollectionTO to = new CollectionTO();
    TransferVOtoTO.transferCollection(getCollectionVO(id, u), to);
    return to;
  }

  private CollectionImeji getCollectionVO(String id, User u) throws ImejiException {
    return new CollectionService().retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
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
    final ItemService itemService = new ItemService();
    final SearchResult result = SearchFactory.create(SEARCH_IMPLEMENTATIONS.ELASTIC).search(
        SearchQueryParser.parseStringQuery(q), null, u,
        ObjectHelper.getURI(CollectionImeji.class, id).toString(), offset, size);
    
    for (final Item vo : itemService.retrieveBatch(result.getResults(), Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, u)) {
      final DefaultItemTO to = new DefaultItemTO();
      TransferVOtoTO.transferDefaultItem(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<DefaultItemTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }

  /**
   * Read all Elements (items and subcollections) of the collection.
   * 
   * @param id
   * @param u
   * @param q
   * @param offset
   * @param size
   * @param sort
   * @return
   * @throws ImejiException
   */
  public SearchResultTO<CollectionElementTO> readElements(String id, User u, String q, int offset,
      int size, SortCriterion sort) throws ImejiException {
    final SearchAndRetrieveFacade facade = new SearchAndRetrieveFacade();
    final SearchResult result = facade.search(SearchQueryParser.parseStringQuery(q),
        ImejiFactory.newCollection().setId(id).build(), u, sort, size, offset);
    final List<CollectionElement> elements =
        facade.retrieveItemsAndCollections(result.getResults(), u);
    final List<CollectionElementTO> tos = elements.stream()
        .map(e -> TransferVOtoTO.toCollectionelementTO(e)).collect(Collectors.toList());
    return new SearchResultTO.Builder<CollectionElementTO>()
        .numberOfRecords(result.getResults().size()).offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }


  @Override
  public CollectionTO create(CollectionTO to, User u) throws ImejiException {
    // toDo: Move to Controller
    final CollectionService cc = new CollectionService();

    final CollectionImeji vo = new CollectionImeji();
    transferCollection(to, vo, CREATE, u);

    URI collectionURI = null;
    collectionURI = cc.create(vo, u).getId();
    return read(CommonUtils.extractIDFromURI(collectionURI), u);
  }

  @Override
  public CollectionTO update(CollectionTO to, User u) throws ImejiException {
    final CollectionService cc = new CollectionService();
    final CollectionImeji vo = getCollectionVO(to.getId(), u);
    transferCollection(to, vo, TRANSFER_MODE.UPDATE, u);
    final CollectionImeji updatedCollection = cc.update(vo, u);
    final CollectionTO newTO = new CollectionTO();
    TransferVOtoTO.transferCollection(updatedCollection, newTO);
    return newTO;
  }

  @Override
  public CollectionTO release(String id, User u) throws ImejiException {
    final CollectionService controller = new CollectionService();
    final CollectionImeji vo =
        controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.releaseWithDefaultLicense(vo, u);
    // Now Read the collection and return it back
    return getCollectionTO(id, u);

  }

  @Override
  public boolean delete(String id, User u) throws ImejiException {
    final CollectionService controller = new CollectionService();
    final CollectionImeji vo =
        controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.delete(vo, u);
    return true;
  }

  @Override
  public CollectionTO withdraw(String id, User u, String discardComment) throws ImejiException {
    final CollectionService controller = new CollectionService();
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
    final CollectionService cc = new CollectionService();
    final List<CollectionTO> tos = new ArrayList<>();
    final SearchResult result =
        SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC)
            .search(SearchQueryParser.parseStringQuery(q), null, u, null, offset, size);
    for (final CollectionImeji vo : cc.retrieve(result.getResults(), u)) {
      final CollectionTO to = new CollectionTO();
      TransferVOtoTO.transferCollection(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<CollectionTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }
}
