/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.search.SearchResult;
import de.mpg.imeji.logic.search.model.SearchQuery;
import de.mpg.imeji.logic.util.MetadataFactory;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.lang.MetadataLabels;
import de.mpg.imeji.presentation.metadata.editors.MetadataEditor;
import de.mpg.imeji.presentation.metadata.editors.MetadataMultipleEditor;
import de.mpg.imeji.presentation.metadata.util.MetadataHelper;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.imeji.presentation.util.ObjectLoader;
import de.mpg.imeji.presentation.util.ProfileHelper;

/**
 * Bean for batch and multiple metadata editor
 * 
 * @author saquet
 */
public class EditItemMetadataBean {
  // objects
  private List<Item> allItems;
  // The Metadateditor which is used to edit
  private MetadataEditor editor = null;
  // The editor before the user made any modification
  private MetadataEditor noChangesEditor = null;
  private MetadataProfile profile = null;
  private Statement statement = null;
  /**
   * the {@link EditorItemBean} used to for the editor and which will be copied to all {@link Item}
   */
  private EditorItemBean editorItem = null;
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
  private SessionBean session = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
  private static final Logger LOGGER = Logger.getLogger(EditItemMetadataBean.class);
  // url parameters
  private String type = "all";
  private String query = "";
  private String collectionId = null;

  /**
   * Bean for batch and multiple metadata editor
   */
  public EditItemMetadataBean() {
    statementMenu = new ArrayList<SelectItem>();
    modeRadio = new ArrayList<SelectItem>();
  }

  /**
   * Initialize all elements of the bean
   * 
   * @throws IOException
   */
  public void init() throws IOException {
    reset();
    try {
      List<String> uris = findItems();
      if (uris != null && !uris.isEmpty()) {
        lockImages(uris);
        // If editing all items, load only the first one
        allItems = "selected".equals(type) ? loaditems(uris) : loaditems(uris.subList(0, 1));
        initProfileAndStatement(allItems);
        if (profile == null) {
          redirectToCollectionItemsPage(collectionId);
          BeanHelper.error(((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
              .getLabel("profile_empty"));
        }
        initStatementsMenu();
        initEmtpyEditorItem();
        initEditor(new ArrayList<Item>(allItems));
        ((MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class)).init(profile);
        initialized = true;
        noChangesEditor = editor.clone();
        initModeMenu();
      } else {
        redirectToCollectionItemsPage(collectionId);
        BeanHelper.error((((SessionBean) BeanHelper.getSessionBean(SessionBean.class)))
            .getMessage("no_items_to_edit"));
      }
    } catch (Exception e) {
      redirectToView();
      BeanHelper.error(
          ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel("error") + " " + e);
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
   * Find the uri of the {@link Item} which are edited
   * 
   * @return
   * @throws IOException
   */
  private List<String> findItems() throws ImejiException {
    if ("selected".equals(type)) {
      return getSelectedItems();
    } else if ("all".equals(type) && query != null && collectionId != null) {
      return searchItems();
    }
    return null;
  }

  /**
   * Load the profile of the images, and set the statement to be edited.
   * 
   * @param items
   */
  private void initProfileAndStatement(List<Item> items) {
    profile = null;
    if (items != null && items.size() > 0) {
      profile =
          ObjectLoader.loadProfile(items.get(0).getMetadataSet().getProfile(), session.getUser());
    }
    statement = getSelectedStatement();
  }

  /**
   * Init the {@link MetadataEditor}
   * 
   * @param items
   */
  private void initEditor(List<Item> items) {
    try {
      isProfileWithStatements = true;
      if (statement != null) {
        editor = new MetadataMultipleEditor(items, profile, getSelectedStatement());
        ((SuggestBean) BeanHelper.getSessionBean(SuggestBean.class)).init(profile);
      } else {
        LOGGER.error("No statement found");
        isProfileWithStatements = false;
        BeanHelper.error(
            ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel("profile_empty"));
      }
    } catch (Exception e) {
      BeanHelper.error(
          ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getLabel("error") + " " + e);
      LOGGER.error("Error init Edit page", e);
    }
  }

  /**
   * Initialize the {@link EditorItemBean} with a new emtpy one;
   */
  private void initEmtpyEditorItem() {
    Item emtpyItem = new Item();
    emtpyItem.getMetadataSets().add(ImejiFactory.newMetadataSet(profile.getId()));
    editorItem = new EditorItemBean(emtpyItem, profile, true);
    editorItem.getMds().addEmtpyValues();
  }

  /**
   * Init the radio select menu with the 3 edit modes (overwrite all values, append new value, add
   * if empty)
   */
  private void initModeMenu() {
    selectedMode = "basic";
    modeRadio = new ArrayList<SelectItem>();
    modeRadio.add(new SelectItem("basic",
        ((SessionBean) BeanHelper.getSessionBean(SessionBean.class)).getMessage("editor_basic")));
    if (this.statement.getMaxOccurs().equals("unbounded")) {
      modeRadio
          .add(new SelectItem("append", ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
              .getMessage("editor_append")));
    }
    modeRadio.add(
        new SelectItem("overwrite", ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
            .getMessage("editor_overwrite")));
  }

  /**
   * Initialize the select menu with the possible statement to edit (i.e. statement of the profiles)
   */
  private void initStatementsMenu() {
    ((MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class)).init(profile);
    statementMenu = new ArrayList<SelectItem>();
    for (Statement s : profile.getStatements()) {
      if (s.getParent() == null) {
        // Add a statement to the menu only if it doen'st have a parent
        // statement. If it has a parent, then it
        // will be editable by choosing the parent in the menu
        statementMenu.add(new SelectItem(s.getId().toString(),
            ((MetadataLabels) BeanHelper.getSessionBean(MetadataLabels.class))
                .getInternationalizedLabels().get(s.getId())));
      }
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
    ItemController itemController = new ItemController();
    return (List<Item>) itemController.retrieveBatch(uris, -1, 0, session.getUser());
  }

  /**
   * Load the selected item from the session
   * 
   * @return
   */
  public List<String> getSelectedItems() {
    List<String> l = new ArrayList<String>(session.getSelected().size());
    for (String uri : session.getSelected()) {
      l.add(uri);
    }
    return l;
  }

  /**
   * Search for item according to the query
   * 
   * @return
   * @throws IOException
   */
  public List<String> searchItems() throws ImejiException {
    SearchQuery sq = SearchQueryParser.parseStringQuery(query);
    ItemController itemController = new ItemController();
    SearchResult sr =
        itemController.search(URI.create(collectionId), sq, null, session.getUser(), null, -1, 0);
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
    if (editor.save()) {
      redirectToView();
    } else {
      reload();
    }
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
      try {
        Locks.lock(new Lock(uris.get(i), session.getUser().getEmail()));
      } catch (Exception e) {
        uris.remove(i);
        lockedImages++;
        i--;
      }
    }
  }

  /**
   * Release the lock on all current {@link Item}
   */
  private void unlockImages() {
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    for (EditorItemBean eib : editor.getItems()) {
      Locks.unLock(new Lock(eib.asItem().getId().toString(), sb.getUser().getEmail()));
    }
  }

  /**
   * Called method when "add to all" button is clicked
   * 
   * @return
   */
  public void addToAll() {
    for (EditorItemBean eib : editor.getItems()) {
      if ("overwrite".equals(selectedMode)) {
        // remove all metadata which have the same statement
        eib.clear(statement);
        // Prepare the emtpy values in which the new values will be
        // added
        eib.getMds().addEmtpyValues();
      } else if ("append".equals(selectedMode)) {
        // Remove all emtpy Metadata
        eib.getMds().trim();
        // Add an emtpy metadata at the position we want to have it
        appendEmtpyMetadata(eib);
      }
      // Add the Metadata which has been entered to the emtpy Metadata
      // with the same statement in the editor
      eib = pasteMetadataIfEmtpy(eib);
    }
    // Make a new Emtpy Metadata of the same statement
    initEmtpyEditorItem();
  }

  /**
   * redirect to previous page
   * 
   * @throws IOException
   */
  public void redirectToView() throws IOException {
    this.reset();
    unlockImages();
    HistorySession hs = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);

    // redirect to view when previous page was upload
    if (hs.getPreviousPage().getUrl().contains("upload")) {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(hs.getPreviousPage().getCompleteUrl().replaceFirst("upload.*", "browse"));

    } else {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(hs.getPreviousPage().getCompleteUrl());
    }
  }

  /**
   * redirect to previous page
   * 
   * @throws IOException
   */
  public void reload() throws IOException {
    HistorySession hs = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
    FacesContext.getCurrentInstance().getExternalContext()
        .redirect(hs.getCurrentPage().getCompleteUrl());
  }

  /**
   * Remove all metadata
   * 
   * @return
   */
  public String clearAll() {
    for (EditorItemBean eib : editor.getItems()) {
      eib.clear(statement);
      eib.getMds().addEmtpyValues();
    }
    return "";
  }

  /**
   * fill all emtpy Metadata of passed {@link EditorItemBean} with the values of the current one
   * 
   * @param im
   * @return
   */
  private EditorItemBean pasteMetadataIfEmtpy(EditorItemBean eib) {
    List<SuperMetadataBean> list =
        fillEmtpyValues(eib.getMds().getTree().getList(), editorItem.getMds().getTree().getList());
    list = SuperMetadataTree.resetPosition(list);
    eib.getMds().initTreeFromList(list);
    return eib;
  }

  /**
   * Fill l1 emtpy metadata with non emtpy metadata from l2
   * 
   * @param l1
   * @param l2
   */
  private List<SuperMetadataBean> fillEmtpyValues(List<SuperMetadataBean> l1,
      List<SuperMetadataBean> l2) {
    List<SuperMetadataBean> filled = new ArrayList<SuperMetadataBean>();
    for (SuperMetadataBean md1 : l1) {
      boolean emtpy1 = MetadataHelper.isEmpty(md1.asMetadata());
      for (SuperMetadataBean md2 : l2) {
        boolean emtpy2 = MetadataHelper.isEmpty(md2.asMetadata());
        if (md1.getStatementId().equals(md2.getStatementId())) {
          if (emtpy1 && !emtpy2)
            filled.add(md2.copy());
          else
            filled.add(md1);
        }
      }
    }
    return filled;
  }

  /**
   * Add an emtpy {@link Metadata} accroding to the current {@link Statement} to the
   * {@link EditorItemBean}. If the {@link EditorItemBean} has already an emtpy {@link Metadata} for
   * this {@link Statement}, then don't had it.
   * 
   * @param eib
   */
  private void appendEmtpyMetadata(EditorItemBean eib) {
    List<SuperMetadataBean> l = eib.getMds().getTree().getList();
    for (Statement st : profile.getStatements())
      l.add(new SuperMetadataBean(MetadataFactory.createMetadata(st), st));
    eib.getMds().initTreeFromList(SuperMetadataTree.resetPosition(l));
  }

  /**
   * Return the {@link Statement} which is currently edited
   * 
   * @return
   */
  public Statement getSelectedStatement() {
    if (profile != null) {
      for (Statement s : profile.getStatements()) {
        if (s.getId().toString().equals(selectedStatementName)) {
          return s;
        }
      }
    }
    return getDefaultStatement();
  }

  /**
   * Return the first {@link Statement} of the current {@link MetadataProfile}
   * 
   * @return
   */
  public Statement getDefaultStatement() {
    if (profile != null && profile.getStatements().iterator().hasNext()) {
      return profile.getStatements().iterator().next();
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
    URI lastParent = ProfileHelper.getLastParent(st, profile);
    return statement.getId().compareTo(st.getId()) == 0
        || (lastParent != null && statement.getId().compareTo(lastParent) == 0);
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

  public MetadataEditor getEditor() {
    return editor;
  }

  public void setEditor(MetadataEditor editor) {
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

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
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
  public EditorItemBean getEditorItem() {
    return editorItem;
  }

  /**
   * @param editorItemBean the editorItemBean to set
   */
  public void setEditorItem(EditorItemBean editorItemBean) {
    this.editorItem = editorItemBean;
  }

  public void redirectToCollectionItemsPage(String collectionId) throws IOException {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);

    FacesContext.getCurrentInstance().getExternalContext()
        .redirect(navigation.getApplicationSpaceUrl() + navigation.getCollectionPath() + "/"
            + ObjectHelper.getId(URI.create(collectionId)) + "/" + navigation.getBrowsePath());
  }
}
