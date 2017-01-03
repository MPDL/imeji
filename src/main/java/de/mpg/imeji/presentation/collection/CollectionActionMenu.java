package de.mpg.imeji.presentation.collection;

import static de.mpg.imeji.presentation.notification.CommonMessages.getSuccessCollectionDeleteMessage;

import java.io.Serializable;
import java.util.Locale;

import javax.faces.event.ValueChangeEvent;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.config.ImejiLicenses;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.model.SearchIndex.SearchFields;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;

/**
 * Class with all methods needed by the Action menu in collection context
 *
 * @author saquet
 *
 */
public class CollectionActionMenu implements Serializable {
  private static final long serialVersionUID = 2439408335958108151L;
  private static final Logger LOGGER = Logger.getLogger(CollectionActionMenu.class);
  private final CollectionImeji collection;
  private final User user;
  private final Locale locale;
  private final String selectedSpaceString;
  private int itemsWithoutLicense = 0;

  public CollectionActionMenu(CollectionImeji collection, User user, Locale locale,
      String selectedSpaceString) {
    this.collection = collection;
    this.user = user;
    this.locale = locale;
    this.selectedSpaceString = selectedSpaceString;
    searchItemsWihoutLicense();
  }

  /**
   * release the {@link CollectionImeji}
   *
   * @return
   */
  public String release() {
    final CollectionController cc = new CollectionController();
    try {
      cc.releaseWithDefaultLicense(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_release", locale));
    } catch (final Exception e) {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_collection_release: " + e.getMessage(), locale));
      LOGGER.error("Error during collection release", e);
    }
    return "pretty:";
  }


  public int getCountOfItemsWithoutLicense() {
    return itemsWithoutLicense;
  }

  /**
   * Search the number of items wihout any license
   */
  protected void searchItemsWihoutLicense() {
    final ItemService controller = new ItemService();
    itemsWithoutLicense = controller
        .search(collection.getId(), SearchQuery.toSearchQuery(new SearchPair(SearchFields.license,
            SearchOperators.REGEX, ImejiLicenses.NO_LICENSE, false)), null, user, null, 0, 0)
        .getNumberOfRecords();
  }

  public String getReleaseMessage() {
    if (itemsWithoutLicense > 0) {
      return itemsWithoutLicense
          + Imeji.RESOURCE_BUNDLE.getMessage("confirmation_release_collection_license", locale);
    }
    return "";
  }

  public String createDOI() {
    try {
      final String doi = UrlHelper.getParameterValue("doi");
      final DoiService doiService = new DoiService();
      if (doi != null) {
        doiService.addDoiToCollection(doi, collection, user);
      } else {
        doiService.addDoiToCollection(collection, user);
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_doi_creation", locale));
    } catch (final UnprocessableError e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), locale));
      LOGGER.error("Error during doi creation", e);
    } catch (final ImejiException e) {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation", locale) + " " + e.getMessage());
      LOGGER.error("Error during doi creation", e);
    }
    return "pretty:";
  }

  /**
   * Delete the {@link CollectionImeji}
   *
   * @return
   */
  public String delete() {
    final CollectionController cc = new CollectionController();
    try {
      cc.delete(collection, user);
      BeanHelper.info(
          getSuccessCollectionDeleteMessage(this.collection.getMetadata().getTitle(), locale));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getLocalizedMessage(), locale));
      LOGGER.error("Error delete collection", e);
    }
    return SessionBean.getPrettySpacePage("pretty:collections", selectedSpaceString);
  }

  /**
   * Discard the {@link CollectionImeji} of this {@link CollectionBean}
   *
   * @return
   * @throws Exception
   */
  public String withdraw() throws Exception {
    final CollectionController cc = new CollectionController();
    try {
      cc.withdraw(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_withdraw", locale));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_withdraw", locale));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding collection:", e);
    }
    return "pretty:";
  }

  public String getDiscardComment() {
    return collection.getDiscardComment();
  }

  public void setDiscardComment(String comment) {
    collection.setDiscardComment(comment);
  }

  /**
   * Listener for the discard comment
   *
   * @param event
   */
  public void discardCommentListener(ValueChangeEvent event) {
    if (event.getNewValue() != null && event.getNewValue().toString().trim().length() > 0) {
      setDiscardComment(event.getNewValue().toString().trim());
    }
  }
}
