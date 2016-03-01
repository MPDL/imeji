/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.util.MetadataFactory;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.metadata.EditorItemBean;
import de.mpg.imeji.presentation.metadata.SuperMetadataBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Abstract call for the {@link Metadata} editors
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class MetadataEditor {
  protected List<EditorItemBean> items;
  protected Statement statement;
  protected MetadataProfile profile;

  // protected Validator validator;
  /**
   * Editor: Edit a list of images for one statement.
   * 
   * @param items
   * @param statement
   */
  public MetadataEditor(List<Item> itemList, MetadataProfile profile, Statement statement,
      boolean addEmtpyValue) {
    this.statement = statement;
    this.profile = profile;
    items = new ArrayList<EditorItemBean>();
    for (Item item : itemList) {
      items.add(new EditorItemBean(item, profile, addEmtpyValue));
    }
    initialize();
  }

  /**
   * Default editor
   */
  public MetadataEditor() {}

  /**
   * Reset all value to empty state
   */
  public void reset() {
    items = new ArrayList<EditorItemBean>();
    statement = null;
    profile = null;
  }

  /**
   * Clone as a {@link MetadataMultipleEditor}
   */
  @Override
  public MetadataEditor clone() {
    MetadataEditor editor = new MetadataMultipleEditor();
    editor.setItems(items);
    editor.setProfile(profile);
    editor.setStatement(statement);
    return editor;
  }

  /**
   * Save the {@link Item} and {@link Metadata} defined in the editor
   */
  public boolean save() {
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    ItemController ic = new ItemController();
    try {
      List<Item> itemList = validateAndFormatItemsForSaving();
      ic.updateBatch(itemList, sb.getUser());
      String str = items.size() + " " + sb.getMessage("success_editor_images");
      if (items.size() == 1) {
        str = sb.getMessage("success_editor_image");
      }
      BeanHelper.info(str);
      return true;

    } catch (Exception e) {
      for (EditorItemBean eib : this.items) {
        eib.getMds().setTree(eib.getMds().getUncutTree());
      }
      BeanHelper.cleanMessages();
      BeanHelper.error(sb.getMessage("error_metadata_edit"));
      List<String> listOfErrors = Arrays.asList(e.getMessage().split(";"));
      for (String errorM : listOfErrors) {
        BeanHelper.error(errorM);
      }
      return false;
    }
  }

  /**
   * Validate and prepare the Items of the editor, so they can be saved
   * 
   * @return
   */
  public List<Item> validateAndFormatItemsForSaving() {
    List<Item> itemList = new ArrayList<Item>();
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);

    if (prepareUpdate()) {
      try {
        addPositionToMetadata();
        for (EditorItemBean eib : items) {
          itemList.add(eib.asItem());
        }
      } catch (Exception e) {
        BeanHelper.error(sb.getMessage("error_metadata_edit") + ": " + e.getLocalizedMessage());
      }
    } else {
      BeanHelper.error(sb.getMessage("error_metadata_edit_no_images"));
    }
    return itemList;

  }

  /**
   * enable ordering for metadata values
   */
  protected void addPositionToMetadata() {
    for (EditorItemBean eib : items) {
      int pos = 0;
      for (SuperMetadataBean smb : eib.getMds().getTree().getList()) {
        smb.setPos(pos);
        pos++;
      }
    }
  }

  public abstract void initialize();

  public abstract boolean prepareUpdate();

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

  public List<EditorItemBean> getItems() {
    return items;
  }

  public int getItemsSize() {
    return items.size();
  }

  public void setItems(List<EditorItemBean> items) {
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
}
