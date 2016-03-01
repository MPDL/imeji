/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.mdProfile;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.model.SelectItem;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.Metadata.Types;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.presentation.collection.CollectionBean.TabType;
import de.mpg.imeji.presentation.collection.CollectionSessionBean;
import de.mpg.imeji.presentation.mdProfile.wrapper.StatementWrapper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;
import de.mpg.imeji.presentation.util.ImejiFactory;
import de.mpg.j2j.misc.LocalizedString;

/**
 * Bean for {@link MetadataProfile} view pages
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class MdProfileBean {
  private MetadataProfile profile = null;
  private TabType tab = TabType.PROFILE;
  private CollectionSessionBean collectionSession = null;
  private List<StatementWrapper> wrappers = null;
  private List<SelectItem> mdTypesMenu = null;
  private Map<URI, Integer> levels;
  private String id = null;
  private List<SelectItem> profilesMenu = null;
  // private String template;
  private int statementPosition = 0;
  /**
   * Position of the dragged element at the start
   */
  private int draggedStatementPosition = 0;
  private int constraintPosition = 0;
  private int labelPosition = 0;
  private static final int MARGIN_PIXELS_FOR_STATEMENT_CHILD = 30;
  /**
   * If a {@link Statement} already used by {@link Metadata} has been removed, return true;
   */
  protected boolean cleanMetadata = false;

  /**
   * initialize a default {@link MdProfileBean}
   */
  public MdProfileBean() {
    collectionSession =
        (CollectionSessionBean) BeanHelper.getSessionBean(CollectionSessionBean.class);
    if (collectionSession.getProfile() == null) {
      collectionSession.setProfile(new MetadataProfile());
    }
    profile = collectionSession.getProfile();
    wrappers = new ArrayList<StatementWrapper>();
    initMenus();
  }

  /**
   * Method called on the html page to trigger the initialization of the bean
   * 
   * @return
   * @throws ImejiException
   * @throws Exception
   */
  public String getInit() {
    parseID();
    initMenus();
    cleanMetadata = false;
    // updateFirstTemplateProfileLabel();
    if (UrlHelper.getParameterBoolean("reset")) {
      reset();
    }
    if (UrlHelper.getParameterBoolean("init") && profile != null) {
      initStatementWrappers(profile);
      if (profile.getStatements().isEmpty()) {
        addFirstStatement();
      }
    }
    return "";
  }

  /**
   * Initialize the menus of the page
   */
  private void initMenus() {
    mdTypesMenu = new ArrayList<SelectItem>();
    for (Metadata.Types t : Metadata.Types.values()) {
      mdTypesMenu.add(new SelectItem(t.getClazzNamespace(),
          ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
              .getLabel("facet_" + t.name().toLowerCase())));
    }
  }

  public void addFirstStatement() {
    Statement firstStatement = ImejiFactory.newStatement();
    getWrappers()
        .add(new StatementWrapper(firstStatement, getProfile().getId(), getLevel(firstStatement)));
  }

  /**
   * Return the label of a {@link Types}
   * 
   * @param uri
   * @return
   */
  public String getTypeLabel(String uri) {
    for (Metadata.Types t : Metadata.Types.values()) {
      if (t.getClazzNamespace().equals(uri)) {
        return ((SessionBean) BeanHelper.getSessionBean(SessionBean.class))
            .getLabel("facet_" + t.name().toLowerCase());
      }
    }
    return uri;
  }

  /**
   * Reset to an empty {@link MetadataProfile}
   */
  public void reset() {
    profile.getStatements().clear();
    wrappers.clear();
    collectionSession.setProfile(profile);
  }

  /**
   * Initialize the {@link StatementWrapper} {@link List}
   * 
   * @param mdp
   */
  protected void initStatementWrappers(MetadataProfile mdp) {
    wrappers.clear();
    levels = new HashMap<URI, Integer>();
    for (Statement st : mdp.getStatements()) {
      wrappers.add(new StatementWrapper(st, mdp.getId(), getLevel(st)));
    }
  }

  /**
   * Parse the id defined in the url
   */
  private void parseID() {
    if (this.getId() == null && this.getProfile().getId() != null) {
      this.setId(this.getProfile().getId().getPath().split("/")[2]);
    }
  }

  /**
   * Return the id of the profile encoded in utf-8
   * 
   * @return
   * @throws UnsupportedEncodingException
   */
  public String getEncodedId() throws UnsupportedEncodingException {
    if (profile != null && profile.getId() != null) {
      return URLEncoder.encode(profile.getId().toString(), "UTF-8");
    } else {
      return "";
    }
  }

  protected String getNavigationString() {
    return "pretty:";
  }

  /**
   * Return the size of the list of statement
   * 
   * @return
   */
  public int getSize() {
    return wrappers.size();
  }

  /**
   * Method called when the user drop a metadata in "insert metadata" area
   */
  public void insertMetadata() {
    StatementWrapper dragged = wrappers.get(getDraggedStatementPosition());
    StatementWrapper dropped =
        wrappers.get(getStatementPosition() > 0 ? getStatementPosition() - 1 : 0);
    boolean moved = insertWrapper(dragged, getStatementPosition());
    if (moved) {
      dragged.getStatement().setParent(dropped.getStatement().getParent());
    }
    reInitWrappers();
  }

  /**
   * Method called when the user drop a metadata at the end of the list
   */
  public void insertLastMetadata() {
    StatementWrapper dragged = wrappers.get(getDraggedStatementPosition());
    boolean moved = insertWrapper(dragged, getStatementPosition());
    if (moved) {
      dragged.getStatement().setParent(null);
    }
    reInitWrappers();
  }

  /**
   * Method called when the user drop a metadata in "insert child" area
   */
  public void insertChild() {
    StatementWrapper dragged = wrappers.get(getDraggedStatementPosition());
    StatementWrapper dropped = wrappers.get(getStatementPosition());
    boolean moved = insertWrapper(dragged, getStatementPosition() + 1);
    if (moved) {
      dropped = setParentOfDropped(dragged, dropped);
      dragged.getStatement().setParent(dropped.getStatement().getId());
    }
    reInitWrappers();
  }

  /**
   * Insert a {@link StatementWrapper} into the {@link List} at the position passed in the
   * parameter. The childs of the wrapper are inserted after it.
   * 
   * @param w - The wrapper to insert
   * @param to - The position to insert in the list
   * @return
   */
  private boolean insertWrapper(StatementWrapper wrapper, int to) {
    // Can't add a statement after a child
    if (!isAParent(wrapper, wrappers.get(to > 0 ? to - 1 : 0))) {
      // Get the childs which must be moved with their parent
      List<StatementWrapper> childs = getChilds(wrapper, false);
      // Increment position after the position where the wrapper has been
      // dropped
      incrementPosition(to, childs.size() + 1);
      // Set the new position of the wrapper
      wrapper.getStatement().setPos(to);
      // Set the new positions of its childs
      int i = 1;
      for (StatementWrapper child : childs) {
        child.getStatement().setPos(to + i);
        i++;
      }
      // Sort the list according to the new positions
      Collections.sort(wrappers);
      // Reset the position
      resetPosition();
      return true;
    }
    return false;
  }

  /**
   * True if the Metadata at this position in the list has a child
   * 
   * @param position
   * @return
   */
  public boolean hasChild(int position) {
    if (position < wrappers.size() && wrappers.get(position).getStatement().getParent() != null) {
      return wrappers.get(position).getStatement().getParent()
          .compareTo(wrappers.get(position - 1).getStatement().getId()) == 0;
    }
    return false;
  }

  /**
   * Increment all position after a position
   * 
   * @param position - The position after to position are incremented
   * @param toIncrement - The value to increment
   */
  private void incrementPosition(int position, int toIncrement) {
    for (StatementWrapper w : wrappers) {
      if (w.getStatement().getPos() >= position)
        w.getStatement().setPos(w.getStatement().getPos() + toIncrement);
    }
  }

  /**
   * Re-initialize the wrappers
   */
  private void reInitWrappers() {
    profile.setStatements(getUnwrappedStatements());
    initStatementWrappers(profile);
  }

  /**
   * Reset the position of the {@link Statement} according to the current order of the
   * {@link StatementWrapper} {@link List}
   */
  private void resetPosition() {
    int i = 0;
    for (StatementWrapper w : wrappers) {
      w.getStatement().setPos(i);
      i++;
    }
  }

  /**
   * The the parent {@link StatementWrapper} of the dropped {@link StatementWrapper}. This might
   * change if its parent is the one being dragged
   * 
   * @param dragged
   * @param dropped
   * @return
   */
  private StatementWrapper setParentOfDropped(StatementWrapper dragged, StatementWrapper dropped) {
    if (isAParent(dragged, dropped)) {
      StatementWrapper firstChild = findFirstChild(dragged);
      if (firstChild != null)
        firstChild.getStatement().setParent(dragged.getStatement().getParent());
    }
    return dropped;
  }

  /**
   * True if the {@link StatementWrapper} parent is one of the parent of the
   * {@link StatementWrapper} child
   * 
   * @param parent
   * @param child
   * @return
   */
  private boolean isAParent(StatementWrapper parent, StatementWrapper child) {
    while (child.getStatement().getParent() != null) {
      if (child.getStatement().getParent().compareTo(parent.getStatement().getId()) == 0) {
        return true;
      } else if (child.getStatement().getPos() - 1 >= 0) {
        StatementWrapper parentOfChild = wrappers.get(child.getStatement().getPos() - 1);
        return isAParent(parent, parentOfChild);
      }
    }
    return false;
  }

  /**
   * Find the first Child of a {@link StatementWrapper} in the list
   * 
   * @param parent
   * @return
   */
  private StatementWrapper findFirstChild(StatementWrapper parent) {
    List<StatementWrapper> l = getChilds(parent, true);
    return (l.size() > 0 ? l.get(0) : null);
  }

  /**
   * REturn all Childs of {@link StatementWrapper}
   * 
   * @param parent
   * @param firstOnly if true, return only the direct childs
   * @return
   */
  private List<StatementWrapper> getChilds(StatementWrapper parent, boolean firstOnly) {
    List<StatementWrapper> l = new ArrayList<StatementWrapper>();
    for (StatementWrapper wrapper : wrappers) {
      if (wrapper.getStatement().getParent() != null
          && wrapper.getStatement().getParent().compareTo(parent.getStatement().getId()) == 0) {
        l.add(wrapper);
        if (!firstOnly) {
          l.addAll(getChilds(wrapper, false));
        }
      }
    }
    return l;
  }

  /**
   * Methods called when the user start to drag a metadata
   */
  public void dragStart() {
    // do nothing, the draggedStatementPosition is set
  }

  /**
   * Move a statement up in statement list
   */
  public void moveUp() {
    Collections.swap(wrappers, getStatementPosition(), getStatementPosition() + 1);
  }

  /**
   * Move a statement down in statement list
   */
  public void moveDown() {
    Collections.swap(wrappers, getStatementPosition() + 1, getStatementPosition());
  }

  /**
   * add a vocabulary according to the position of the clicked button
   */
  public void addVocabulary() {
    wrappers.get(getStatementPosition()).setVocabularyString("--");
  }

  /**
   * remove a vocabulary
   */
  public void removeVocabulary() {
    wrappers.get(getStatementPosition()).setVocabularyString(null);
  }

  /**
   * Get the level (how many parents does it have) of a {@link Statement}
   * 
   * @param st
   */
  protected int getLevel(Statement st) {
    if (!levels.containsKey(st.getId())) {
      if (st.getParent() != null && levels.get(st.getParent()) != null) {
        levels.put(st.getId(), (levels.get(st.getParent()) + MARGIN_PIXELS_FOR_STATEMENT_CHILD));
      } else {
        levels.put(st.getId(), 0);
      }
    }
    return levels.get(st.getId());
  }

  /**
   * Find the next {@link Statement} in the {@link Statement} list which have the same level, which
   * means, the first {@link Statement} which is not a child
   * 
   * @param st
   * @return
   */
  private int findNextStatementWithSameLevel(Statement st) {
    int i = 0;
    for (i = getStatementPosition() + 1; i < wrappers.size(); i++) {
      if (wrappers.get(i).getLevel() == getLevel(st)) {
        // a statement with the same level have been found, return
        // position
        return i;
      } else if (wrappers.get(i).getLevel() < getLevel(st)) {
        // in statement with an higher posotion hsa been found, i.e. we
        // reached the end of the list of childs.
        // Return then this current position
        return i;
      }
    }
    // We reached the end of the list
    return i;
  }

  /**
   * Called by add statement button. Add a new statement to the profile. The position of the new
   * statement is defined by the button position
   */
  public void addStatement() {
    if (wrappers.isEmpty()) {
      wrappers.add(new StatementWrapper(ImejiFactory.newStatement(), profile.getId(), 0));
    } else {
      Statement previousStatement = wrappers.get(getStatementPosition()).getStatement();
      Statement newStatement = ImejiFactory.newStatement(previousStatement.getParent());
      wrappers.add(findNextStatementWithSameLevel(previousStatement),
          new StatementWrapper(newStatement, profile.getId(), getLevel(newStatement)));
    }
  }

  /**
   * Called by remove statement button. If the statement is not used by an imeji item, remove it,
   * according to the position of the button. If the statement is used, display a warning message in
   * a panel
   */
  public void removeStatement() {
    if (!wrappers.get(getStatementPosition()).isUsed()) {
      removeStatementWithChilds(wrappers.get(getStatementPosition()));
    } else {
      wrappers.get(getStatementPosition()).setShowRemoveWarning(true);
    }
  }

  /**
   * Called by add statement child button. Add a new statement to the profile as a child of the
   * previous statement. The position of the new statement is defined by the button position
   */
  public void addStatementChild() {
    if (!wrappers.isEmpty()) {
      URI parent = wrappers.get(getStatementPosition()).getStatement().getId();
      Statement newChild = ImejiFactory.newStatement(parent);
      wrappers.add(getStatementPosition() + 1,
          new StatementWrapper(newChild, profile.getId(), getLevel(newChild)));
    }
  }

  /**
   * Remove a {@link Statement} even if it is used by a an item. All {@link Metadata} using this
   * {@link Statement} are then removed.
   */
  public void forceRemoveStatement() {
    removeStatementWithChilds(wrappers.get(getStatementPosition()));
    cleanMetadata = true;
  }

  /**
   * Remove a {@link StatementWrapper} and all its childs from the {@link MetadataProfile}
   */
  private void removeStatementWithChilds(StatementWrapper parent) {
    List<StatementWrapper> toDelete = getChilds(parent, false);
    toDelete.add(wrappers.get(getStatementPosition()));
    List<StatementWrapper> l = new ArrayList<StatementWrapper>();
    for (StatementWrapper sw : wrappers) {
      if (!toDelete.contains(sw)) {
        l.add(sw);
      }
    }
    wrappers = l;
  }

  /**
   * Close the panel with warning information
   */
  public void closeRemoveWarning() {
    wrappers.get(getStatementPosition()).setShowRemoveWarning(false);
  }

  /**
   * called by add label button
   */
  public void addLabel() {
    wrappers.get(getStatementPosition()).getStatement().getLabels()
        .add(new LocalizedString("", ""));
  }

  /**
   * Called by remove label button
   */
  public void removeLabel() {
    ((List<LocalizedString>) wrappers.get(getStatementPosition()).getStatement().getLabels())
        .remove(getLabelPosition());
  }

  /**
   * Called by add constraint button
   */
  public void addConstraint() {
    Statement st = wrappers.get(getStatementPosition()).getAsStatement();
    if (getConstraintPosition() >= st.getLiteralConstraints().size()) {
      ((List<String>) st.getLiteralConstraints()).add("");
    } else {
      ((List<String>) st.getLiteralConstraints()).add(getConstraintPosition() + 1, "");
    }
    collectionSession.setProfile(profile);
  }

  /**
   * Called by remove constraint button
   */
  public void removeConstraint() {
    Statement st = wrappers.get(getStatementPosition()).getAsStatement();
    ((List<String>) st.getLiteralConstraints()).remove(getConstraintPosition());
    collectionSession.setProfile(profile);
  }

  /**
   * getter
   * 
   * @return
   */
  public int getConstraintPosition() {
    return constraintPosition;
  }

  /**
   * setter
   * 
   * @param constraintPosition
   */
  public void setConstraintPosition(int constraintPosition) {
    this.constraintPosition = constraintPosition;
  }

  /**
   * getter
   * 
   * @return
   */
  public MetadataProfile getProfile() {
    return profile;
  }

  /**
   * setter
   * 
   * @param profile
   */
  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  /**
   * getter
   * 
   * @return
   */
  public TabType getTab() {
    return tab;
  }

  /**
   * setter
   * 
   * @param tab
   */
  public void setTab(TabType tab) {
    this.tab = tab;
  }

  /**
   * @return the statementPosition
   */
  public int getStatementPosition() {
    return statementPosition;
  }

  /**
   * @param statementPosition the statementPosition to set
   */
  public void setStatementPosition(int statementPosition) {
    this.statementPosition = statementPosition;
  }

  /**
   * return the {@link List} of {@link StatementWrapper} as a {@link List} of {@link Statement}
   * 
   * @return
   */
  public List<Statement> getUnwrappedStatements() {
    List<Statement> l = new ArrayList<Statement>();
    for (StatementWrapper w : getWrappers()) {
      l.add(w.getAsStatement());
    }
    return l;
  }

  /**
   * getter
   * 
   * @return
   */
  public List<StatementWrapper> getWrappers() {
    return wrappers;
  }

  /**
   * setter
   * 
   * @param statements
   */
  public void setWrappers(List<StatementWrapper> wrappers) {
    this.wrappers = wrappers;
  }

  /**
   * getter
   * 
   * @return
   */
  public List<SelectItem> getMdTypesMenu() {
    return mdTypesMenu;
  }

  /**
   * setter
   * 
   * @param mdTypesMenu
   */
  public void setMdTypesMenu(List<SelectItem> mdTypesMenu) {
    this.mdTypesMenu = mdTypesMenu;
  }

  /**
   * getter
   * 
   * @return
   */
  public String getId() {
    return id;
  }

  /**
   * setter
   * 
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * getter
   * 
   * @return
   */
  public List<SelectItem> getProfilesMenu() {
    return profilesMenu;
  }

  /**
   * setter
   * 
   * @param profilesMenu
   */
  public void setProfilesMenu(List<SelectItem> profilesMenu) {
    this.profilesMenu = profilesMenu;
  }

  /**
   * getter
   * 
   * @return
   */
  public int getLabelPosition() {
    return labelPosition;
  }

  /**
   * setter
   * 
   * @param labelPosition
   */
  public void setLabelPosition(int labelPosition) {
    this.labelPosition = labelPosition;
  }

  /**
   * @param draggedStatementPosition the draggedStatementPosition to set
   */
  public void setDraggedStatementPosition(int draggedStatementPosition) {
    this.draggedStatementPosition = draggedStatementPosition;
  }

  /**
   * @return the draggedStatementPosition
   */
  public int getDraggedStatementPosition() {
    return draggedStatementPosition;
  }

  public String getMdTypesMenuAsString() {
    String s = "";
    for (SelectItem si : mdTypesMenu) {
      s += si.getValue() + "," + si.getLabel() + "|";
    }
    return s;
  }
}
