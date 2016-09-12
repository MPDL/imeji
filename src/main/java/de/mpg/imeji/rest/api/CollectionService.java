package de.mpg.imeji.rest.api;

import static com.google.common.base.Strings.isNullOrEmpty;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.transferCollection;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.CREATE;
import static de.mpg.imeji.rest.transfer.ReverseTransferObjectFactory.TRANSFER_MODE.UPDATE;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import de.mpg.imeji.exceptions.BadRequestException;
import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.ItemController;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.search.Search.SearchObjectTypes;
import de.mpg.imeji.logic.search.factory.SearchFactory;
import de.mpg.imeji.logic.search.factory.SearchFactory.SEARCH_IMPLEMENTATIONS;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.util.ObjectHelper;
import de.mpg.imeji.rest.helper.CommonUtils;
import de.mpg.imeji.rest.helper.ProfileCache;
import de.mpg.imeji.rest.to.CollectionProfileTO;
import de.mpg.imeji.rest.to.CollectionProfileTO.METHOD;
import de.mpg.imeji.rest.to.CollectionTO;
import de.mpg.imeji.rest.to.SearchResultTO;
import de.mpg.imeji.rest.to.defaultItemTO.DefaultItemTO;
import de.mpg.imeji.rest.transfer.MetadataTransferHelper;
import de.mpg.imeji.rest.transfer.TransferObjectFactory;

/**
 * API Service for {@link CollectionTO}
 *
 * @author bastiens
 *
 */
public class CollectionService implements API<CollectionTO> {
  private CollectionTO getCollectionTO(String id, User u) throws ImejiException {
    CollectionTO to = new CollectionTO();
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
    ProfileCache profileCache = new ProfileCache();
    List<DefaultItemTO> tos = new ArrayList<>();
    ItemController controller = new ItemController();
    SearchResult result = SearchFactory.create(SEARCH_IMPLEMENTATIONS.ELASTIC).search(
        SearchQueryParser.parseStringQuery(q), null, u,
        ObjectHelper.getURI(CollectionImeji.class, id).toString(), null, offset, size);
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

  public Object readItemTemplate(String id, User u) throws ImejiException, IOException {
    return MetadataTransferHelper.readItemTemplateForProfile(id, null, u);
  }


  @Override
  public CollectionTO create(CollectionTO to, User u) throws ImejiException {
    // toDo: Move to Controller
    CollectionController cc = new CollectionController();
    ProfileController pc = new ProfileController();

    MetadataProfile mp = null;
    String profileId = to.getProfile().getId();
    String method = to.getProfile().getMethod();

    // create new profile (take default)
    if (!isNullOrEmpty(profileId)) {
      try {
        mp = pc.retrieve(ObjectHelper.getURI(MetadataProfile.class, profileId), u);
      } catch (ImejiException e) {
        throw new UnprocessableError(
            "Can not find the metadata profile you have referenced in the JSON body");

      }
    }

    CollectionImeji vo = new CollectionImeji();
    transferCollection(to, vo, CREATE, u);

    URI collectionURI = null;
    collectionURI = cc.create(vo, mp, u, cc.getProfileCreationMethod(method), null).getId();
    return read(CommonUtils.extractIDFromURI(collectionURI), u);
  }

  @Override
  public CollectionTO update(CollectionTO to, User u) throws ImejiException {
    ProfileController pc = new ProfileController();
    CollectionController cc = new CollectionController();

    CollectionImeji vo = getCollectionVO(to.getId(), u);
    MetadataProfile originalMp = pc.retrieve(vo.getProfile(), u);

    String hasStatements = (originalMp == null) ? ""
        : ((originalMp.getStatements().size() > 0)
            ? " Existing metadata profile has already defined metadata elements. It is not allowed to update it: remove the profileId from your input."
            : "");

    // profile is defined
    CollectionProfileTO profTO = to.getProfile();
    String profileId = (profTO != null) ? profTO.getId() : "";
    String method = (profTO != null) ? profTO.getMethod() : METHOD.REFERENCE.name();

    MetadataProfile mp = null;

    transferCollection(to, vo, UPDATE, u);
    // profileId is filled
    if (!isNullOrEmpty(profileId)) {
      try {
        mp = pc.retrieve(profileId, u);
      } catch (ImejiException e) {
        throw new UnprocessableError(
            "Can not retrieve the metadata profile provided in the JSON body with id: " + profileId
                + hasStatements);
      }

      if (originalMp == null && profileId != null || !profileId.equals(originalMp.getIdString())) {
        if (!METHOD.COPY.toString().equals(method) && !METHOD.REFERENCE.toString().equals(method)) {
          throw new BadRequestException("Wrong metadata profile update method: " + method
              + " ! Allowed values are {copy, reference}. ");
        }
      }
    }

    CollectionImeji updatedCollection =
        cc.updateWithProfile(vo, mp, u, cc.getProfileCreationMethod(method));
    CollectionTO newTO = new CollectionTO();
    TransferObjectFactory.transferCollection(updatedCollection, newTO);
    return newTO;
  }

  @Override
  public CollectionTO release(String id, User u) throws ImejiException {
    CollectionController controller = new CollectionController();
    CollectionImeji vo = controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.release(vo, u);
    // Now Read the collection and return it back
    return getCollectionTO(id, u);

  }

  @Override
  public boolean delete(String id, User u) throws ImejiException {
    CollectionController controller = new CollectionController();
    CollectionImeji vo = controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
    controller.delete(vo, u);
    return true;
  }

  @Override
  public CollectionTO withdraw(String id, User u, String discardComment) throws ImejiException {
    CollectionController controller = new CollectionController();
    CollectionImeji vo = controller.retrieve(ObjectHelper.getURI(CollectionImeji.class, id), u);
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
    CollectionController cc = new CollectionController();
    List<CollectionTO> tos = new ArrayList<>();
    SearchResult result =
        SearchFactory.create(SearchObjectTypes.COLLECTION, SEARCH_IMPLEMENTATIONS.ELASTIC)
            .search(SearchQueryParser.parseStringQuery(q), null, u, null, null, offset, size);
    for (CollectionImeji vo : cc.retrieveBatchLazy(result.getResults(), -1, 0, u)) {
      CollectionTO to = new CollectionTO();
      TransferObjectFactory.transferCollection(vo, to);
      tos.add(to);
    }
    return new SearchResultTO.Builder<CollectionTO>().numberOfRecords(result.getResults().size())
        .offset(offset).results(tos).query(q).size(size)
        .totalNumberOfRecords(result.getNumberOfRecords()).build();
  }
}
