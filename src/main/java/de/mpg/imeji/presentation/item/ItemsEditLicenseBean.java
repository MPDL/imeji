package de.mpg.imeji.presentation.item;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.component.LicenseEditor;

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
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  private boolean overwriteLicenses = false;
  private LicenseEditor licenseEditor;
  private String collectionId;
  private boolean releasedCollection = false;

  @PostConstruct
  public void init() throws ImejiException {
    collectionId = UrlHelper.getParameterValue("collection");
    setLicenseEditor(new LicenseEditor(getLocale()));
    releasedCollection = findIfCollectionIsReleased();
  }

  /**
   * True if the collection or one of the selected item is released/withdrawn
   * 
   * @return
   * @throws ImejiException
   */
  private boolean findIfCollectionIsReleased() throws ImejiException {
    if (StringHelper.isNullOrEmptyTrim(collectionId)) {
      for (Item item : retrieveSelectedItems()) {
        if (!item.getStatus().equals(Status.PENDING)) {
          return true;
        }
      }
      return false;
    } else {
      return !new CollectionController()
          .retrieveLazy(ObjectHelper.getURI(CollectionImeji.class, collectionId), getSessionUser())
          .getStatus().equals(Status.PENDING);
    }
  }

  /**
   * Save the editor: add licenses to all items
   * 
   * @throws ImejiException
   * @throws IOException
   */
  public void save() throws ImejiException, IOException {
    List<Item> items = !StringHelper.isNullOrEmptyTrim(collectionId)
        ? retrieveAllCollectionsItem(collectionId) : retrieveSelectedItems();
    addLicense(items);
    save(items);
    cancel();
  }

  /**
   * Save the Items
   * 
   * @param items
   * @throws ImejiException
   */
  private void save(List<Item> items) throws ImejiException {
    new ItemBusinessController().updateBatch(items, getSessionUser());
  }

  /**
   * Add the selected License to all the items
   * 
   * @param items
   */
  private List<Item> addLicense(List<Item> items) {
    List<Item> itemsWithNewLicense = new ArrayList<>();
    for (Item item : items) {
      if (overwriteLicenses || !hasLicense(item)) {
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
      item.setLicenses(Arrays.asList(licenseEditor.getLicense()));
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
    ItemBusinessController controller = new ItemBusinessController();
    return (List<Item>) controller.retrieveBatchLazy(selectedItems, -1, 0, getSessionUser());
  }

  /**
   * Retrieve all items of a collection
   * 
   * @param collectionId
   * @return
   * @throws ImejiException
   */
  private List<Item> retrieveAllCollectionsItem(String collectionId) throws ImejiException {
    ItemBusinessController controller = new ItemBusinessController();
    List<String> uris = controller.search(ObjectHelper.getURI(CollectionImeji.class, collectionId),
        null, null, getSessionUser(), getSpaceId(), -1, 0).getResults();
    return (List<Item>) controller.retrieveBatchLazy(uris, -1, 0, getSessionUser());
  }

  /**
   * Return to the previous page
   * 
   * @throws IOException
   */
  public void cancel() throws IOException {
    redirect(getHistory().getPreviousPage().getCompleteUrlWithHistory());
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
