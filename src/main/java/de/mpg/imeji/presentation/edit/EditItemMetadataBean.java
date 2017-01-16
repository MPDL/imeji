/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.edit;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.item.ItemService;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.search.model.SearchResult;
import de.mpg.imeji.logic.statement.StatementService;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.util.MetadataUtil;
import de.mpg.imeji.presentation.beans.MetadataLabels;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.edit.editors.AbstractMetadataEditor;
import de.mpg.imeji.presentation.edit.editors.MultipleEditor;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for batch and multiple metadata editor
 *
 * @author saquet
 */
@ManagedBean(name = "EditItemMetadataBean")
@ViewScoped
public class EditItemMetadataBean extends SuperBean {
  private static final long serialVersionUID = 120352070993734848L;
  private static final Logger LOGGER = Logger.getLogger(EditItemMetadataBean.class);
  // objects
  private List<Item> allItems;
  // The Metadateditor which is used to edit
  private AbstractMetadataEditor editor = null;
  // The editor before the user made any modification
  private AbstractMetadataEditor noChangesEditor = null;
  private Statement statement = null;
  /**
   * the {@link ItemWrapper} used to for the editor and which will be copied to all {@link Item}
   */
  private ItemWrapper editorItem = null;
  // menus
  private List<SelectItem> statementMenu = null;
  private String selectedStatementName = null;
  private List<SelectItem> modeRadio = null;
  private String selectedMode = "basic";
  // other
  private int mdPosition;
  private int imagePosition;
  private boolean isProfileWithStatements = true;
  private int lockedImages = 0;
  private boolean initialized = false;
  // url parameters
  private String type = "all";
  private String query = "";
  private String collectionId = null;
  private MetadataLabels metadataLabels;
  @ManagedProperty(value = "#{SessionBean.selected}")
  private List<String> selectedItems;
  List<String> uris = new ArrayList<>();

  /**
   * Bean for batch and multiple metadata editor
   */
  public EditItemMetadataBean() {
    statementMenu = new ArrayList<SelectItem>();
    modeRadio = new ArrayList<SelectItem>();
  }

  /**
   * Read the url paramameters when the page is first called. This method is called directly from
   * the xhtml page
   *
   * @return
   */
  public String getUrlParameters() {
    type = UrlHelper.getParameterValue("type");
    query = UrlHelper.getParameterValue("q");
    collectionId = UrlHelper.getParameterValue("c");
    return "";
  }

  /**
   * Initialize all elements of the bean
   *
   * @throws IOException
   */
  public void init() throws IOException {
    reset();
    uris = new ArrayList<>();
    try {
      uris = findItems();
      if (uris != null && !uris.isEmpty()) {
        lockImages(uris);
        // If editing all items, load only the first one
        allItems = "selected".equals(type) ? loaditems(uris) : loaditems(uris.subList(0, 1));
        statement = getSelectedStatement();
        metadataLabels = new MetadataLabels(allItems, getLocale());
        initStatementsMenu();
        initEditor(new ArrayList<Item>(allItems));
        initialized = true;
        noChangesEditor = editor.clone();
        initModeMenu();
      } else {
        redirectToCollectionItemsPage(collectionId);
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("no_items_to_edit", getLocale()));
      }
    } catch (final Exception e) {
      redirectToView();
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("error", getLocale()) + " " + e.getMessage());
      LOGGER.error("Error init Edit page", e);
    }
  }

  /**
   * Set all pages element to their default values
   */
  public void reset() {
    initialized = false;
    statementMenu = new ArrayList<SelectItem>();
    modeRadio = new ArrayList<SelectItem>();
    if (editor != null) {
      editor.reset();
    }
    statement = null;
  }

  /**
   * Go back to the previous page
   *
   * @return
   * @throws IOException
   */
  public void cancel() throws IOException {
    redirectToView();
  }



  /**
   * Find the uri of the {@link Item} which are edited
   *
   * @return
   * @throws IOException
   */
  private List<String> findItems() throws ImejiException {
    if ("selected".equals(type)) {
      return selectedItems;
    } else if ("all".equals(type) && query != null && collectionId != null) {
      return searchItems();
    }
    return null;
  }


  /**
   * Init the {@link AbstractMetadataEditor}
   *
   * @param items
   */
  private void initEditor(List<Item> items) {
    try {
      isProfileWithStatements = true;
      if (statement != null) {
        editor = new MultipleEditor(items, getSelectedStatement(), getSessionUser(), getLocale());
      } else {
        LOGGER.error("No statement found");
        isProfileWithStatements = false;
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("profile_empty", getLocale()));
      }
    } catch (final Exception e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getLabel("error", getLocale()) + " " + e);
      LOGGER.error("Error init Edit page", e);
    }
  }

  /**
   * Init the radio select menu with the 3 edit modes (overwrite all values, append new value, add
   * if empty)
   */
  private void initModeMenu() {
    selectedMode = "basic";
    modeRadio = new ArrayList<SelectItem>();
    modeRadio.add(
        new SelectItem("basic", Imeji.RESOURCE_BUNDLE.getMessage("editor_basic", getLocale())));
    modeRadio.add(
        new SelectItem("append", Imeji.RESOURCE_BUNDLE.getMessage("editor_append", getLocale())));
    modeRadio.add(new SelectItem("overwrite",
        Imeji.RESOURCE_BUNDLE.getMessage("editor_overwrite", getLocale())));
  }

  /**
   * Initialize the select menu with the possible statement to edit (i.e. statement of the profiles)
   */
  private void initStatementsMenu() {
    statementMenu = new ArrayList<SelectItem>();
    try {
      for (final Statement s : new StatementService().searchAndRetrieve(null, null,
          getSessionUser(), -1, 0)) {
        statementMenu.add(new SelectItem(s.getId().toString(),
            metadataLabels.getInternationalizedLabels().get(s.getId())));
      }
    } catch (final ImejiException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  /**
   * Change the statement to edit
   *
   * @return
   */
  public String changeStatement() {
    statement = getSelectedStatement();
    // Reset the original items
    // initEditor(new ArrayList<Item>(allItems));
    // initEmtpyEditorItem();
    editor = noChangesEditor.clone();
    initModeMenu();
    return "";
  }

  /**
   * Set to the original state
   *
   * @return
   * @throws IOException
   */
  public String resetChanges() throws IOException {
    init();
    return "";
  }

  /**
   * Load the list of items
   *
   * @param uris
   * @return
   * @throws ImejiException
   */
  public List<Item> loaditems(List<String> uris) throws ImejiException {
    final ItemService itemController = new ItemService();
    return (List<Item>) itemController.retrieveBatch(uris, -1, 0, getSessionUser());
  }

  /**
   * Search for item according to the query
   *
   * @return
   * @throws IOException
   */
  public List<String> searchItems() throws ImejiException {
    final SearchQuery sq = SearchQueryParser.parseStringQuery(query);
    final ItemService itemController = new ItemService();
    final SearchResult sr =
        itemController.search(URI.create(collectionId), sq, null, getSessionUser(), -1, 0);
    return sr.getResults();
  }

  /**
   * For batch edit: Add the same values to all images and save.
   *
   * @return
   * @throws IOException
   * @throws ImejiException
   */
  public String addToAllSaveAndRedirect() throws IOException, ImejiException {
    // First, re-initialize the editor with all items (for batch, editor has
    // been initialized with only one item)
    initEditor(new ArrayList<Item>(loaditems(findItems())));
    addToAll();
    saveAndRedirect();
    return "";
  }

  /**
   * For batch edit: Add the same values to all images and save.
   *
   * @return
   * @throws Exception
   */
  public void addToAllSave() throws Exception {
    // First, re-initialize the editor with all items (for batch, editor has
    // been initialized with only one item)
    initEditor(new ArrayList<Item>(loaditems(findItems())));
    addToAll();
    save();
  }

  /**
   * For the Multiple Edit: Save the current values
   *
   * @return
   * @throws IOException
   */
  public void saveAndRedirect() throws IOException {
    try {
      editor.save();
      redirectToView();
      return;
    } catch (final UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error saving batch editor", e);
    } catch (final ImejiException e) {
      BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_metadata_edit", getLocale()));
      LOGGER.error("Error saving batch editor", e);
    }
    reload();
  }

  /**
   * For the Multiple Edit: Save the current values
   *
   * @return
   * @throws Exception
   */
  public void save() throws Exception {
    editor.save();
    reload();
  }

  /**
   * Lock the {@link Item} which are currently in the editor. This prevent other users to make
   * concurrent modification.
   *
   * @param items
   */
  private void lockImages(List<String> uris) {
    lockedImages = 0;
    for (int i = 0; i < uris.size(); i++) {
      Locks.lock(new Lock(uris.get(i), getSessionUser().getEmail()));
    }
  }

  /**
   * Release the lock on all current {@link Item}
   */
  private void unlockImages(List<String> uris) {
    try {
      for (final String uri : uris) {
        Locks.unLock(new Lock(uri, getSessionUser().getEmail()));
      }
    } catch (final Exception e) {
      LOGGER.error("Error unlocking items", e);
    }

  }

  /**
   * Called method when "add to all" button is clicked
   *
   * @return
   */
  public void addToAll() {
    for (ItemWrapper eib : editor.getItems()) {
      if ("overwrite".equals(selectedMode)) {
        // remove all metadata which have the same statement
        eib.clear(statement);
      } else if ("append".equals(selectedMode)) {
      }
      // Add the Metadata which has been entered to the emtpy Metadata
      // with the same statement in the editor
      eib = pasteMetadataIfEmtpy(eib);
    }
  }

  /**
   * redirect to previous page
   *
   * @throws IOException
   */
  public void redirectToView() throws IOException {
    this.reset();
    unlockImages(uris);
    // redirect to view when previous page was upload
    if (getHistory().getPreviousPage().getUrl().contains("upload")) {
      redirect(getHistory().getPreviousPage().getCompleteUrl().replaceFirst("upload.*", "browse"));
    } else {
      redirect(getHistory().getPreviousPage().getCompleteUrl());
    }
  }

  /**
   * redirect to previous page
   *
   * @throws IOException
   */
  public void reload() throws IOException {
    redirect(getHistory().getCurrentPage().getCompleteUrl());
  }

  /**
   * Remove all metadata
   *
   * @return
   */
  public String clearAll() {
    for (final ItemWrapper eib : editor.getItems()) {
      eib.clear(statement);
    }
    return "";
  }

  /**
   * fill all emtpy Metadata of passed {@link ItemWrapper} with the values of the current one
   *
   * @param im
   * @return
   */
  private ItemWrapper pasteMetadataIfEmtpy(ItemWrapper eib) {
    // TODO
    // List<MetadataWrapper> list =
    // fillEmtpyValues(eib.getMds().getTree().getList(), editorItem.getMds().getTree().getList());
    // list = MetadataWrapperTree.resetPosition(list);
    // eib.getMds().initTreeFromList(list);
    return eib;
  }

  /**
   * Fill l1 emtpy metadata with non emtpy metadata from l2
   *
   * @param l1
   * @param l2
   */
  private List<MetadataWrapper> fillEmtpyValues(List<MetadataWrapper> l1,
      List<MetadataWrapper> l2) {
    final List<MetadataWrapper> filled = new ArrayList<MetadataWrapper>();
    for (final MetadataWrapper md1 : l1) {
      final boolean emtpy1 = MetadataUtil.isEmpty(md1.asMetadata());
      for (final MetadataWrapper md2 : l2) {
        final boolean emtpy2 = MetadataUtil.isEmpty(md2.asMetadata());
        if (md1.getStatementId().equals(md2.getStatementId())) {
          if (emtpy1 && !emtpy2) {
            filled.add(md2.copy());
          } else {
            filled.add(md1);
          }
        }
      }
    }
    return filled;
  }


  /**
   * Return the {@link Statement} which is currently edited
   *
   * @return
   */
  public Statement getSelectedStatement() {
    try {
      for (final Statement s : new StatementService().searchAndRetrieve(null, null,
          getSessionUser(), -1, 0)) {
        if (s.getId().toString().equals(selectedStatementName)) {
          return s;
        }
      }
    } catch (final ImejiException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
    return null;
  }

  /**
   * True if the {@link Statement} can be edited
   *
   * @param st
   * @return
   */
  public boolean isEditableStatement(Statement st) {
    return statement.getId().compareTo(st.getId()) == 0;
  }

  public int getMdPosition() {
    return mdPosition;
  }

  public void setMdPosition(int mdPosition) {
    this.mdPosition = mdPosition;
  }

  public int getImagePosition() {
    return imagePosition;
  }

  public void setImagePosition(int imagePosition) {
    this.imagePosition = imagePosition;
  }

  public AbstractMetadataEditor getEditor() {
    return editor;
  }

  public void setEditor(AbstractMetadataEditor editor) {
    this.editor = editor;
  }

  public List<SelectItem> getStatementMenu() {
    return statementMenu;
  }

  public boolean getDisplayStatementMenu() {
    return statementMenu.size() <= 1 ? Boolean.FALSE : Boolean.TRUE;
  }

  public void setStatementMenu(List<SelectItem> statementMenu) {
    this.statementMenu = statementMenu;
  }

  public String getSelectedStatementName() {
    return selectedStatementName;
  }

  public void setSelectedStatementName(String selectedStatementName) {
    this.selectedStatementName = selectedStatementName;
  }

  public List<SelectItem> getModeRadio() {
    return modeRadio;
  }

  public void setModeRadio(List<SelectItem> modeRadio) {
    this.modeRadio = modeRadio;
  }

  public String getSelectedMode() {
    return selectedMode;
  }

  public void setSelectedMode(String selectedMode) {
    this.selectedMode = selectedMode;
  }

  public String getEditType() {
    return type;
  }

  public void setEditType(String editType) {
    this.type = editType;
  }

  public Statement getStatement() {
    return statement;
  }

  public void setStatement(Statement statement) {
    this.statement = statement;
  }

  public boolean isProfileWithStatements() {
    return isProfileWithStatements;
  }

  public void setProfileWithStatements(boolean isProfileWithStatements) {
    this.isProfileWithStatements = isProfileWithStatements;
  }

  public int getLockedImages() {
    return lockedImages;
  }

  public void setLockedImages(int lockedImages) {
    this.lockedImages = lockedImages;
  }

  public void setInitialized(boolean initialized) {
    this.initialized = initialized;
  }

  public boolean isInitialized() {
    return initialized;
  }

  /**
   * @return the editorItemBean
   */
  public ItemWrapper getEditorItem() {
    return editorItem;
  }

  /**
   * @param editorItemBean the editorItemBean to set
   */
  public void setEditorItem(ItemWrapper editorItemBean) {
    this.editorItem = editorItemBean;
  }

  public void redirectToCollectionItemsPage(String collectionId) throws IOException {
    redirect(getNavigation().getApplicationUrl() + getNavigation().getCollectionPath() + "/"
        + ObjectHelper.getId(URI.create(collectionId)) + "/" + getNavigation().getBrowsePath());
  }

  public MetadataLabels getMetadataLabels() {
    return metadataLabels;
  }

  public List<String> getSelectedItems() {
    return selectedItems;
  }

  public void setSelectedItems(List<String> selectedItems) {
    this.selectedItems = selectedItems;
  }
}
