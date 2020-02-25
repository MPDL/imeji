package de.mpg.imeji.presentation.collection;

import static de.mpg.imeji.presentation.notification.CommonMessages.getSuccessCollectionDeleteMessage;

import java.io.IOException;
import java.io.Serializable;
import java.util.Locale;

import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.doi.DoiService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.ImejiLicenses;
import de.mpg.imeji.logic.model.SearchFields;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.search.model.SearchOperators;
import de.mpg.imeji.logic.search.model.SearchPair;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.presentation.navigation.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Class with all methods needed by the Action menu in collection context
 *
 * @author saquet
 *
 */
public class CollectionActionMenu implements Serializable {
  private static final long serialVersionUID = 2439408335958108151L;
  private static final Logger LOGGER = LogManager.getLogger(CollectionActionMenu.class);
  private final CollectionImeji collection;
  private final User user;
  private final Locale locale;
  private int itemsWithoutLicense = 0;

  public CollectionActionMenu(CollectionImeji collection, User user, Locale locale) {
    this.collection = collection;
    this.user = user;
    this.locale = locale;
  }

  /**
   * release the {@link CollectionImeji}
   *
   * @return
   */
  public String release() {
    final CollectionService cc = new CollectionService();
    try {
      cc.releaseWithDefaultLicense(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_release", locale));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_release: " + e.getMessage(), locale));
      LOGGER.error("Error during collection release", e);
    }
    return "pretty:";
  }

  public int getCountOfItemsWithoutLicense() {
    return itemsWithoutLicense;
  }

  /**
   * Search the number of items without any license
   */
  public void searchItemsWihoutLicense() {
    final ItemService controller = new ItemService();
    itemsWithoutLicense = controller.search(collection.getId(),
        SearchQuery.toSearchQuery(new SearchPair(SearchFields.license, SearchOperators.EQUALS, ImejiLicenses.NO_LICENSE, false)), null,
        user, 0, 0).getNumberOfRecords();
  }

  public String getReleaseMessage() {
    if (itemsWithoutLicense > 0) {
      return itemsWithoutLicense + Imeji.RESOURCE_BUNDLE.getMessage("confirmation_release_collection_license", locale);
    }
    return "";
  }

  /**
   * Create a new DOI. Method is called from JSF.
   * 
   * @return next URL to navigate to
   */

  public String createDOI() {
    return createDOI(null);
  }

  /**
   * Add a DOI to a collection. Method is called from JSF.
   * 
   * @param newDOI
   * @return next URL to navigate to
   */
  public String createDOI(String newDOI) {

    if (newDOI != null) {
      addProvidedDOI(newDOI);
    } else {
      addMPDLDoi();
    }
    return "pretty:";
  }

  /**
   * Add a provided DOI to a collection
   * 
   * @param newDOI
   */
  private void addProvidedDOI(String newDOI) {

    if (StringHelper.isNullOrEmptyTrim(newDOI)) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation_error_doi_emtpy", locale));
      LOGGER.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation_error_doi_emtpy", locale));
      return;
    }
    final DoiService doiService = new DoiService();
    // copy current collection's DOI in order to restore it in case of exceptions
    String currentDOI = this.collection.getDoi();
    try {
      collection.setDoi(newDOI);
      doiService.addProvidedDoiToCollection(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_doi_creation", locale));
    } catch (final ImejiException imejiException) {
      // restore
      collection.setDoi(currentDOI);
      // exception handling
      if (imejiException instanceof UnprocessableError) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(imejiException.getMessage(), locale));
        LOGGER.error(Imeji.RESOURCE_BUNDLE.getMessage(imejiException.getMessage(), locale));
      } else if (imejiException instanceof ImejiExceptionWithUserMessage) {
        ImejiExceptionWithUserMessage exceptionWithMessage = (ImejiExceptionWithUserMessage) imejiException;
        String userMessage =
            Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation", locale) + " " + exceptionWithMessage.getUserMessage(locale);
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      } else {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation", locale));
        LOGGER.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_creation", locale) + " " + imejiException.getMessage());
      }

    }

  }

  /**
   * Add a DOI from MPDL DOI Service to a collection
   */
  private void addMPDLDoi() {

    try {
      final DoiService doiService = new DoiService();
      doiService.addMPDLDoiToCollection(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_doi_creation", locale));
    } catch (final UnprocessableError e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), locale));
      LOGGER.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getMessage(), locale));
    } catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
      String userMessage =
          Imeji.RESOURCE_BUNDLE.getMessage("error_doi_generation", locale) + " " + exceptionWithMessage.getUserMessage(locale);
      BeanHelper.error(userMessage);
      if (exceptionWithMessage.getMessage() != null) {
        LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
      } else {
        LOGGER.error(userMessage, exceptionWithMessage);
      }
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_generation", locale));
      LOGGER.error(Imeji.RESOURCE_BUNDLE.getMessage("error_doi_generation", locale) + " " + e.getMessage());
    }
  }

  /**
   * Delete the {@link CollectionImeji}
   *
   * @return
   * @throws IOException
   */
  public void delete() throws IOException {
    final CollectionService cc = new CollectionService();
    try {
      cc.delete(collection, user);
      BeanHelper.info(getSuccessCollectionDeleteMessage(this.collection.getTitle(), locale));
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage(e.getLocalizedMessage(), locale));
      LOGGER.error("Error delete collection", e);
    }
    redirectToParent(collection);
  }

  /**
   * Discard the {@link CollectionImeji} of this {@link CollectionBean}
   *
   * @return
   * @throws Exception
   */
  public void withdraw() throws Exception {
    final CollectionService cc = new CollectionService();
    try {
      cc.withdraw(collection, user);
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_collection_withdraw", locale));
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_withdraw", locale));
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error discarding collection:", e);
    }
    redirectToParent(collection);
  }

  /**
   * Redirect to the parent collection. If no parent collection, go back to the collections page
   * 
   * @param collection
   * @throws IOException
   */
  private void redirectToParent(CollectionImeji collection) throws IOException {
    final Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    final String redirectUrl = collection.isSubCollection() ? navigation.getCollectionUrl() + ObjectHelper.getId(collection.getCollection())
        : navigation.getCollectionsUrl();
    FacesContext.getCurrentInstance().getExternalContext().redirect(redirectUrl);
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

  /**
   * Check if the discard comment is empty or contains only spaces
   * 
   * @return
   */
  public boolean discardCommentEmpty() {

    String discardComment = collection.getDiscardComment();
    if (discardComment == null || "".equals(discardComment) || "".equals(discardComment.trim())) {
      return true;
    } else {
      return false;
    }
  }

}
