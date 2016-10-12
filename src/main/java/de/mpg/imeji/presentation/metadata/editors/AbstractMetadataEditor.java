/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.editors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.License;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.util.MetadataFactory;
import de.mpg.imeji.presentation.component.LicenseEditor;
import de.mpg.imeji.presentation.metadata.ItemWrapper;

/**
 * Abstract call for the {@link Metadata} editors
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class AbstractMetadataEditor {
  protected List<ItemWrapper> items;
  protected Statement statement;
  protected MetadataProfile profile;
  protected User sessionUser;
  protected Locale locale;
  private LicenseEditor licenseEditor;

  /**
   * Editor: Edit a list of images for one statement.
   *
   * @param items
   * @param statement
   */
  public AbstractMetadataEditor(List<Item> itemList, MetadataProfile profile, Statement statement,
      User sessionUser, Locale locale) {
    this.statement = statement;
    this.profile = profile;
    this.locale = locale;
    this.sessionUser = sessionUser;
    items = new ArrayList<ItemWrapper>();
    for (Item item : itemList) {
      items.add(new ItemWrapper(item, profile, true));
    }
  }

  /**
   * Default editor
   */
  public AbstractMetadataEditor() {
    // cosntructor...
  }

  /**
   * Reset all value to empty state
   */
  public void reset() {
    items = new ArrayList<ItemWrapper>();
    statement = null;
    profile = null;
  }

  /**
   * Clone as a {@link MultipleEditor}
   */
  @Override
  public AbstractMetadataEditor clone() {
    AbstractMetadataEditor editor = new MultipleEditor();
    editor.setItems(items);
    editor.setProfile(profile);
    editor.setStatement(statement);
    editor.setSessionUser(sessionUser);
    editor.setLocale(locale);
    return editor;
  }

  /**
   * Save the {@link Item} and {@link Metadata} defined in the editor
   *
   * @throws ImejiException
   */
  public void save() throws ImejiException {
    ItemBusinessController ic = new ItemBusinessController();
    List<Item> itemList = validateAndFormatItemsForSaving();
    ic.updateBatch(itemList, sessionUser);
  }

  /**
   * Validate and prepare the Items of the editor, so they can be saved
   *
   * @return
   */
  public List<Item> validateAndFormatItemsForSaving() {
    List<Item> itemList = new ArrayList<Item>();
    for (ItemWrapper eib : items) {
      Item item = eib.asItem();
      addLicense(item);
      itemList.add(item);
    }
    return itemList;
  }

  /**
   * Add the selected license to the item if not null
   * 
   * @param item
   */
  private void addLicense(Item item) {
    if (licenseEditor != null) {
      License lic = licenseEditor.getLicense();
      if (lic != null && !lic.isEmtpy()) {
        item.getLicenses().add(lic);
      }
    }
  }

  /**
   * Create a new Metadata according to current Editor configuration.
   *
   * @return
   */
  protected Metadata newMetadata() {
    if (statement != null) {
      return MetadataFactory.createMetadata(statement);
    }
    return null;
  }

  public List<ItemWrapper> getItems() {
    return items;
  }

  public int getItemsSize() {
    return items.size();
  }

  public void setItems(List<ItemWrapper> items) {
    this.items = items;
  }

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  /**
   * @return the sessionUser
   */
  public User getSessionUser() {
    return sessionUser;
  }

  /**
   * @param sessionUser the sessionUser to set
   */
  public void setSessionUser(User sessionUser) {
    this.sessionUser = sessionUser;
  }

  /**
   * @return the locale
   */
  public Locale getLocale() {
    return locale;
  }

  /**
   * @param locale the locale to set
   */
  public void setLocale(Locale locale) {
    this.locale = locale;
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
}
