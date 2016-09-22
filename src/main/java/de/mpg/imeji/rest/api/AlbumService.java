package de.mpg.imeji.rest.api;

import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.transferAlbum;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.CREATE;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.UPDATE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.helper.ProfileCache;
import de.mpg.imeji.rest.to.AlbumTO;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.transfer.TransferObjectFactory;

/**
 * Service for {@link AlbumTO}
 *
 * @author bastiens
 *
 */
public class AlbumService implements API<AlbumTO> {

  private AlbumTO getAlbumTO(AlbumController controller, String id, User u) throws ImejiException {
    AlbumTO to = new AlbumTO();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    TransferObjectFactory.transferAlbum(vo, to);
    return to;
  }

  @Override
  public AlbumTO read(String id, User u) throws ImejiException {
    AlbumController controller = new AlbumController();
    return getAlbumTO(controller, id, u);
  }

  /**
   * Read all the items of an album according to search query. Response is done with the default
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
    ProfileCache profileCache = new ProfileCache();
    List<DefaultItemTO> tos = new ArrayList<>();
    ItemBusinessController controller = new ItemBusinessController();
    SearchResult result = SearchFactory.create(SEARCH_IMPLEMENTATIONS.ELASTIC).search(
        SearchQueryParser.parseStringQuery(q), null, u,
        ObjectHelper.getURI(Album.class, id).toString(), null, offset, size);
    for (Item vo : controller.retrieveBatch(result.getResults(), -1, 0, u)) {
      DefaultItemTO to = new DefaultItemTO();
      TransferObjectFactory.transferDefaultItem(vo, to,
          profileCache.read(vo.getMetadataSet().getProfile()));
      tos.add(to);
    }
    return new SearchResultTO.Builder<DefaultItemTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }

  @Override
  public AlbumTO create(AlbumTO to, User u) throws ImejiException {
    AlbumController ac = new AlbumController();
    Album vo = new Album();
    transferAlbum(to, vo, CREATE, u);
    URI albumURI;
    albumURI = ac.create(vo, u).getId();
    return read(CommonUtils.extractIDFromURI(albumURI), u);
  }

  @Override
  public AlbumTO update(AlbumTO to, User u) throws ImejiException {
    AlbumController ac = new AlbumController();
    Album vo = ac.retrieve(ObjectHelper.getURI(Album.class, to.getId()), u);
    if (vo == null) {
      throw new UnprocessableError("Album not found");
    }
    transferAlbum(to, vo, UPDATE, u);
    AlbumTO newTO = new AlbumTO();
    TransferObjectFactory.transferAlbum(ac.update(vo, u), newTO);
    return newTO;
  }

  @Override
  public boolean delete(String id, User u) throws ImejiException {
    AlbumController controller = new AlbumController();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    controller.delete(vo, u);
    return true;
  }

  @Override
  public AlbumTO release(String id, User u) throws ImejiException {
    AlbumController controller = new AlbumController();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    controller.release(vo, u);
    return getAlbumTO(controller, id, u);
  }

  @Override
  public AlbumTO withdraw(String id, User u, String discardComment) throws ImejiException {
    AlbumController controller = new AlbumController();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    vo.setDiscardComment(discardComment);
    controller.withdraw(vo, u);
    return getAlbumTO(controller, id, u);
  }

  @Override
  public void share(String id, String userId, List<String> roles, User u) throws ImejiException {}

  public List<String> addItems(String id, User u, List<String> itemIds) throws ImejiException {
    AlbumController controller = new AlbumController();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    List<String> itemUris = new ArrayList<>();
    // Convert Ids to Uris
    for (String itemId : itemIds) {
      itemUris.add(ObjectHelper.getURI(Item.class, itemId).toASCIIString());
    }
    return controller.addToAlbum(vo, itemUris, u);
  }

  public boolean removeItems(String id, User u, List<String> itemIds, boolean removeAll)
      throws ImejiException {
    AlbumController controller = new AlbumController();
    Album vo = controller.retrieve(ObjectHelper.getURI(Album.class, id), u);
    List<String> itemUris = new ArrayList<>();
    if (!removeAll) {
      // Convert Ids to Uris
      for (String itemId : itemIds) {
        itemUris.add(ObjectHelper.getURI(Item.class, itemId).toASCIIString());
      }
    } else {
      for (URI uri : vo.getImages()) {
        itemUris.add(uri.toString());
      }
    }
    controller.removeFromAlbum(vo, itemUris, u);
    return true;
  }

  @Override
  public void unshare(String id, String userId, List<String> roles, User u) throws ImejiException {}

  @Override
  public SearchResultTO<AlbumTO> search(String q, int offset, int size, User u)
      throws ImejiException {
    AlbumController controller = new AlbumController();
    List<AlbumTO> tos = new ArrayList<>();
    SearchResult result =
        SearchFactory.create(SearchObjectTypes.ALBUM, SEARCH_IMPLEMENTATIONS.ELASTIC)
            .search(SearchQueryParser.parseStringQuery(q), null, u, null, null, offset, size);
    for (Album vo : controller.retrieveBatchLazy(result.getResults(), u, -1, 0)) {
      AlbumTO to = new AlbumTO();
      TransferObjectFactory.transferAlbum(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<AlbumTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }

}
