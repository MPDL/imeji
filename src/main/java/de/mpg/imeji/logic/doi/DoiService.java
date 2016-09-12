package de.mpg.imeji.logic.doi;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotAllowedError;
import de.mpg.imeji.exceptions.NotFoundException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.exceptions.WorkflowException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.doi.models.DOICollection;
import de.mpg.imeji.logic.doi.util.DOIUtil;
import de.mpg.imeji.logic.security.authorization.Authorization;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.Properties.Status;

/**
 * Service for using MPDL DOI Service
 *
 * @author bastiens
 *
 */
public final class DoiService {

  public static final String DOI_URL_RESOLVER = "http://dx.doi.org/";
  private final CollectionController collectionController = new CollectionController();
  private final Authorization authorization = new Authorization();

  /**
   * Add a DOI to a {@link CollectionImeji}
   *
   * @param coll
   * @param user
   * @throws ImejiException
   */
  public void addDoiToCollection(CollectionImeji coll, User user) throws ImejiException {
    isValidDOIOperation(coll, user);
    String doiServiceUrl = Imeji.CONFIG.getDoiServiceUrl();
    String doiUser = Imeji.CONFIG.getDoiUser();
    String doiPassword = Imeji.CONFIG.getDoiPassword();
    String doi = getNewDoi(coll, doiServiceUrl, doiUser, doiPassword);
    coll.setDoi(doi);
    collectionController.update(coll, user);
  }

  /**
   *
   * @param doi
   * @param collection
   * @param user
   * @throws ImejiException
   */
  public void addDoiToCollection(String doi, CollectionImeji collection, User user)
      throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(doi)) {
      throw new UnprocessableError("error_doi_creation_error_doi_emtpy");
    }
    isValidDOIOperation(collection, user);
    collection.setDoi(doi);
    collectionController.update(collection, user);
  }

  /**
   * Get a DOI for a {@link CollectionImeji}
   *
   * @param col
   * @throws Exception
   */
  private String getNewDoi(CollectionImeji col, String doiServiceUrl, String doiUser,
      String doiPassword) throws ImejiException {
    DOICollection dcol = DOIUtil.transformToDO(col);
    String xml = DOIUtil.convertToXML(dcol);
    String doi =
        DOIUtil.makeDOIRequest(doiServiceUrl, doiUser, doiPassword, getCollectionUrl(col), xml);
    return doi;

  }

  private String getCollectionUrl(CollectionImeji col) {
    return Imeji.PROPERTIES.getApplicationURL() + "collection/" + col.getIdString();
  }

  /**
   * Valid if the user is allowed to add a DOI to this Collection
   *
   * @param coll
   * @param user
   * @throws ImejiException
   */
  private void isValidDOIOperation(CollectionImeji coll, User user) throws ImejiException {
    if (coll == null) {
      throw new NotFoundException("Collection does not exists");
    }
    if (!authorization.administrate(user, coll)) {
      throw new NotAllowedError("You are not allowed to add a DOI to this collection");
    }
    if (!Status.RELEASED.equals(coll.getStatus())) {
      throw new WorkflowException("Collection has to be released to create a DOI");
    }
  }

}
