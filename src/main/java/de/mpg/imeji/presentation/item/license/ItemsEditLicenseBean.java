package de.mpg.imeji.presentation.item.license;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.ImejiExceptionWithUserMessage;
import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.core.collection.CollectionService;
import de.mpg.imeji.logic.core.item.ItemService;
import de.mpg.imeji.logic.model.CollectionImeji;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.Properties.Status;
import de.mpg.imeji.logic.search.Search;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean to edit the licenses of items
 *
 * @author saquet
 *
 */
@ManagedBean(name = "ItemsEditLicenseBean")
@ViewScoped
public class ItemsEditLicenseBean extends SuperBean {
  private static final long serialVersionUID = 6463190740530976180L;
  private static final Logger LOGGER = LogManager.getLogger(ItemsEditLicenseBean.class);
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  private boolean overwriteLicenses = false;
  private LicenseEditor licenseEditor;
  private String collectionId;
  private boolean releasedCollection = false;

  @PostConstruct
  public void init() {
    try {
      collectionId = UrlHelper.getParameterValue("collection");
      releasedCollection = findIfCollectionIsReleased();
      setLicenseEditor(new LicenseEditor(getLocale(), !releasedCollection));
    } catch (final Exception e) {
      LOGGER.error("Error initializing edit items license page", e);
    }

  }

  /**
   * True if the collection or one of the selected item is released/withdrawn
   *
   * @return
   * @throws ImejiException
   */
  private boolean findIfCollectionIsReleased() throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(collectionId)) {
      for (final Item item : retrieveSelectedItems()) {
        if (!item.getStatus().equals(Status.PENDING)) {
          return true;
        }
      }
      return false;
    } else {
      return !new CollectionService().retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, collectionId), getSessionUser()).getStatus()
          .equals(Status.PENDING);
    }
  }

  /**
   * Save the editor: add licenses to all items
   *
   * @throws ImejiException
   * @throws IOException
   */
  public void save() throws ImejiException, IOException {
    List<Item> items = !StringHelper.isNullOrEmptyTrim(collectionId) ? retrieveAllCollectionsItem(collectionId) : retrieveSelectedItems();
    items = addLicense(items);
    try {
      save(items);
      BeanHelper.addMessage(getLicenseName() + " " + Imeji.RESOURCE_BUNDLE.getLabel("licenses_added_to", getLocale()) + " " + items.size()
          + " " + Imeji.RESOURCE_BUNDLE.getLabel("items", getLocale()));
    } 
    catch (final ImejiExceptionWithUserMessage exceptionWithMessage) {
        String userMessage = Imeji.RESOURCE_BUNDLE.getMessage(exceptionWithMessage.getMessageLabel(), getLocale());
        BeanHelper.error(userMessage);
        if (exceptionWithMessage.getMessage() != null) {
          LOGGER.error(exceptionWithMessage.getMessage(), exceptionWithMessage);
        } else {
          LOGGER.error(userMessage, exceptionWithMessage);
        }
      }
    catch (final ImejiException e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error saving items", e);
    }
    cancel();
  }

  private String getLicenseName() {
    if (StringHelper.isNullOrEmptyTrim(licenseEditor.getLicense().getName())) {
      return LicenseEditor.NO_LICENSE;
    }
    return StringHelper.isNullOrEmptyTrim(licenseEditor.getLicense().getLabel()) ? licenseEditor.getLicense().getUrl()
        : licenseEditor.getLicense().getLabel();
  }

  /**
   * Save the Items
   *
   * @param items
   * @throws ImejiException
   */
  private void save(List<Item> items) throws ImejiException {
    new ItemService().updateBatch(items, getSessionUser());
  }

  /**
   * Add the selected License to all the items
   *
   * @param items
   */
  private List<Item> addLicense(List<Item> items) {
    final List<Item> itemsWithNewLicense = new ArrayList<>();
    for (final Item item : items) {
      if (overwriteLicenses || !hasLicense(item) || !item.getStatus().equals(Status.PENDING)) {
        itemsWithNewLicense.add(addLicense(item));
      }
    }
    return itemsWithNewLicense;
  }

  /**
   * Add the selected License to the item
   *
   * @param item
   */
  private Item addLicense(Item item) {
    if (item.getStatus().equals(Status.PENDING)) {
      item.setLicenses(new ArrayList<>(Arrays.asList(licenseEditor.getLicense())));
    } else {
      if (item.getLicenses() == null) {
        item.setLicenses(new ArrayList<>());
      }
      item.getLicenses().add(licenseEditor.getLicense());
    }
    return item;
  }

  /**
   * True if the Item has at least one license
   *
   * @param item
   * @return
   */
  private boolean hasLicense(Item item) {
    return item.getLicenses() != null && item.getLicenses().size() > 0;
  }

  /**
   * retrieve the selectedItems
   *
   * @return
   * @throws ImejiException
   */
  private List<Item> retrieveSelectedItems() throws ImejiException {
    final ItemService itemService = new ItemService();
    return (List<Item>) itemService.retrieveBatch(selectedItems, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
  }

  /**
   * Retrieve all items of a collection
   *
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  private List<Item> retrieveAllCollectionsItem(String collectionId) throws ImejiException {
    final ItemService itemService = new ItemService();
    final List<String> uris = itemService.search(ObjectHelper.getURI(CollectionImeji.class, collectionId), null, null, getSessionUser(),
        Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX).getResults();
    return (List<Item>) itemService.retrieveBatch(uris, Search.GET_ALL_RESULTS, Search.SEARCH_FROM_START_INDEX, getSessionUser());
  }

  /**
   * Return to the previous page
   *
   * @throws IOException
   */
  public void cancel() throws IOException {
    redirect(getPreviousPage().getCompleteUrlWithHistory());
  }

  /**
   * @return the overwriteLicenses
   */
  public boolean isOverwriteLicenses() {
    return overwriteLicenses;
  }

  /**
   * @param overwriteLicenses the overwriteLicenses to set
   */
  public void setOverwriteLicenses(boolean overwriteLicenses) {
    this.overwriteLicenses = overwriteLicenses;
  }

  /**
   * @return the licenseEditor
   */
  public LicenseEditor getLicenseEditor() {
    return licenseEditor;
  }

  /**
   * @param licenseEditor the licenseEditor to set
   */
  public void setLicenseEditor(LicenseEditor licenseEditor) {
    this.licenseEditor = licenseEditor;
  }

  /**
   * @return the selectedItems
   */
  public List<String> getSelectedItems() {
    return selectedItems;
  }

  /**
   * @param selectedItems the selectedItems to set
   */
  public void setSelectedItems(List<String> selectedItems) {
    this.selectedItems = selectedItems;
  }

  /**
   * @return the releasedCollection
   */
  public boolean isReleasedCollection() {
    return releasedCollection;
  }

  /**
   * @param releasedCollection the releasedCollection to set
   */
  public void setReleasedCollection(boolean releasedCollection) {
    this.releasedCollection = releasedCollection;
  }

}
