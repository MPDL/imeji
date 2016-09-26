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

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.ProfileController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata.Types;
import de.mpg.imeji.logic.vo.util.ImejiFactory;
import de.mpg.imeji.presentation.beans.SuperBean;
import de.mpg.imeji.presentation.collection.CollectionBean.TabType;
import de.mpg.imeji.presentation.collection.CollectionSessionBean;
import de.mpg.imeji.presentation.mdProfile.wrapper.StatementWrapper;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.util.LocalizedString;

/**
 * Bean for {@link MetadataProfile} view pages
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "MdProfileBean")
@ViewScoped
public class MdProfileBean extends SuperBean {
  private static final long serialVersionUID = -1845604633134947188L;
  private static final Logger LOGGER = Logger.getLogger(MdProfileBean.class);
  private String id = null;
  private MetadataProfile profile = null;
  private TabType tab = TabType.PROFILE;
  @ManagedProperty(value = "#{CollectionSessionBean}")
  private CollectionSessionBean collectionSession;
  private List<StatementWrapper> wrappers = null;
  private List<SelectItem> mdTypesMenu = null;
  private Map<URI, Integer> levels;
  private List<SelectItem> profilesMenu = null;
  /**
   * Position of the dragged element
   */
  private int draggedStatementPosition = 0;
  private static final int MARGIN_PIXELS_FOR_STATEMENT_CHILD = 30;
  /**
   * If a {@link Statement} already used by {@link Metadata} has been removed, return true;
   */
  protected boolean cleanMetadata = false;

  @PostConstruct
  public void init() {
    this.id = UrlHelper.getParameterValue("id");
    retrieveProfile();
    if (collectionSession.getProfile() == null) {
      collectionSession.setProfile(profile);
    }
    specificSetup();
  }

  /**
   * Retrieve the profile
   */
  protected void retrieveProfile() {
    if (id != null) {
      try {
        this.profile = new ProfileController().retrieve(id, getSessionUser());
      } catch (ImejiException e) {
        LOGGER.error("Error retrieving profile", e);
        BeanHelper.error(e.getMessage());;
      }
    } else {
      BeanHelper.error(
          Imeji.RESOURCE_BUNDLE.getLabel("error", getLocale()) + "  No profile Id found in URL");
    }
  }

  /**
   * Method called on the html page to trigger the initialization of the bean
   *
   * @return
   * @throws ImejiException
   * @throws Exception
   */
  public void specificSetup() {
    wrappers = new ArrayList<StatementWrapper>();
    initMenus();
    cleanMetadata = false;
    // updateFirstTemplateProfileLabel();
    if (UrlHelper.getParameterBoolean("reset")) {
      reset();
    }
    if (profile != null) {
      initStatementWrappers(profile);
      if (profile.getStatements().isEmpty()) {
        addFirstStatement();
      }
    }
  }

  /**
   * Initialize the menus of the page
   */
  private void initMenus() {
    mdTypesMenu = new ArrayList<SelectItem>();
    for (Metadata.Types t : Metadata.Types.values()) {
      mdTypesMenu.add(new SelectItem(t.getClazzNamespace(),
          Imeji.RESOURCE_BUNDLE.getLabel("facet_" + t.name().toLowerCase(), getLocale())));
    }
  }

  public void addFirstStatement() {
    Statement firstStatement = ImejiFactory.newStatement();
    getWrappers().add(new StatementWrapper(firstStatement, getProfile().getId(),
        getLevel(firstStatement), getLocale()));
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
        return Imeji.RESOURCE_BUNDLE.getLabel("facet_" + t.name().toLowerCase(), getLocale());
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
      wrappers.add(new StatementWrapper(st, mdp.getId(), getLevel(st), getLocale()));
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
  public void insertMetadata(int position) {
    StatementWrapper dragged = wrappers.get(draggedStatementPosition);
    StatementWrapper dropped = wrappers.get(position > 0 ? position - 1 : 0);
    boolean moved = insertWrapper(dragged, position);
    if (moved) {
      dragged.getStatement().setParent(dropped.getStatement().getParent());
    }
    reInitWrappers();
  }

  /**
   * Method called when the user drop a metadata at the end of the list
   */
  public void insertLastMetadata(int position) {
    StatementWrapper dragged = wrappers.get(draggedStatementPosition);
    boolean moved = insertWrapper(dragged, position);
    if (moved) {
      dragged.getStatement().setParent(null);
    }
    reInitWrappers();
  }

  /**
   * Method called when the user drop a metadata in "insert child" area
   */
  public void insertChild(int position) {
    StatementWrapper dragged = wrappers.get(draggedStatementPosition);
    StatementWrapper dropped = wrappers.get(position);
    boolean moved = insertWrapper(dragged, position + 1);
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
      if (w.getStatement().getPos() >= position) {
        w.getStatement().setPos(w.getStatement().getPos() + toIncrement);
      }
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
      if (firstChild != null) {
        firstChild.getStatement().setParent(dragged.getStatement().getParent());
      }
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
  public void dragStart(int position) {
    this.draggedStatementPosition = position;
  }


  /**
   * add a vocabulary according to the position of the clicked button
   */
  public void addVocabulary(int position) {
    wrappers.get(position).setVocabularyString("--");
  }

  /**
   * remove a vocabulary
   */
  public void removeVocabulary(int position) {
    wrappers.get(position).setVocabularyString(null);
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
  private int findNextStatementWithSameLevel(Statement st, int position) {
    int i = 0;
    for (i = position + 1; i < wrappers.size(); i++) {
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
  public void addStatement(int position) {
    if (wrappers.isEmpty()) {
      wrappers
          .add(new StatementWrapper(ImejiFactory.newStatement(), profile.getId(), 0, getLocale()));
    } else {
      Statement previousStatement = wrappers.get(position).getStatement();
      Statement newStatement = ImejiFactory.newStatement(previousStatement.getParent());
      wrappers.add(findNextStatementWithSameLevel(previousStatement, position),
          new StatementWrapper(newStatement, profile.getId(), getLevel(newStatement), getLocale()));
    }
  }

  /**
   * Called by remove statement button. If the statement is not used by an imeji item, remove it,
   * according to the position of the button. If the statement is used, display a warning message in
   * a panel
   */
  public void removeStatement(int position) {
    if (!wrappers.get(position).isUsed()) {
      removeStatementWithChilds(wrappers.get(position), position);
    } else {
      wrappers.get(position).setShowRemoveWarning(true);
    }
  }

  /**
   * Called by add statement child button. Add a new statement to the profile as a child of the
   * previous statement. The position of the new statement is defined by the button position
   */
  public void addStatementChild(int position) {
    if (!wrappers.isEmpty()) {
      URI parent = wrappers.get(position).getStatement().getId();
      Statement newChild = ImejiFactory.newStatement(parent);
      wrappers.add(position + 1,
          new StatementWrapper(newChild, profile.getId(), getLevel(newChild), getLocale()));
    }
  }

  /**
   * Remove a {@link Statement} even if it is used by a an item. All {@link Metadata} using this
   * {@link Statement} are then removed.
   */
  public void forceRemoveStatement(int position) {
    removeStatementWithChilds(wrappers.get(position), position);
    cleanMetadata = true;
  }

  /**
   * Remove a {@link StatementWrapper} and all its childs from the {@link MetadataProfile}
   */
  private void removeStatementWithChilds(StatementWrapper parent, int position) {
    List<StatementWrapper> toDelete = getChilds(parent, false);
    toDelete.add(wrappers.get(position));
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
  public void closeRemoveWarning(int position) {
    wrappers.get(position).setShowRemoveWarning(false);
  }

  /**
   * called by add label button
   */
  public void addLabel(int position) {
    wrappers.get(position).getStatement().getLabels().add(new LocalizedString("", ""));
  }

  /**
   * Called by remove label button
   */
  public void removeLabel(int position, int labelPosition) {
    ((List<LocalizedString>) wrappers.get(position).getStatement().getLabels())
        .remove(labelPosition);
  }

  /**
   * Called by add constraint button
   */
  public void addConstraint(int position, int constrainPosition) {
    Statement st = wrappers.get(position).getAsStatement();
    if (constrainPosition >= st.getLiteralConstraints().size()) {
      ((List<String>) st.getLiteralConstraints()).add("");
    } else {
      ((List<String>) st.getLiteralConstraints()).add(constrainPosition + 1, "");
    }
    collectionSession.setProfile(profile);
  }

  /**
   * Called by remove constraint button
   */
  public void removeConstraint(int position, int constrainPosition) {
    Statement st = wrappers.get(position).getAsStatement();
    ((List<String>) st.getLiteralConstraints()).remove(constrainPosition);
    collectionSession.setProfile(profile);
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



  public String getMdTypesMenuAsString() {
    String s = "";
    for (SelectItem si : mdTypesMenu) {
      s += si.getValue() + "," + si.getLabel() + "|";
    }
    return s;
  }

  public CollectionSessionBean getCollectionSession() {
    return collectionSession;
  }

  public void setCollectionSession(CollectionSessionBean collectionSession) {
    this.collectionSession = collectionSession;
  }
}
