/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.metadata;

import java.util.ArrayList;
import java.util.List;

import javax.faces.context.FacesContext;

import de.mpg.imeji.logic.auth.util.AuthUtil;
import de.mpg.imeji.logic.concurrency.locks.Lock;
import de.mpg.imeji.logic.concurrency.locks.Locks;
import de.mpg.imeji.logic.controller.ItemController;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.Metadata;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.presentation.history.HistorySession;
import de.mpg.imeji.presentation.metadata.editors.SimpleImageEditor;
import de.mpg.imeji.presentation.metadata.util.SuggestBean;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.util.BeanHelper;

/**
 * Metadata Editor for the detail item page
 * 
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class SingleEditBean {
  private Item item = null;
  private MetadataProfile profile = null;
  private SimpleImageEditor editor = null;
  private String toggleState = "displayMd";
  private int mdPosition = 0;
  private List<SuperMetadataBean> metadataList;

  /**
   * Constructor
   * 
   * @param im
   * @param profile
   * @param pageUrl
   */
  public SingleEditBean(Item im, MetadataProfile profile) {
    item = im;
    this.profile = profile;
    init();
  }

  /**
   * Check in the url if the editor should be automatically shown
   * 
   * @return
   */
  public String getCheckToggleState() {
    toggleState = "displayMd";
    if (UrlHelper.getParameterBoolean("edit")) {
      showEditor();
    }
    return "";
  }

  /**
   * Initialize the page
   */
  public void init() {
    editor = new SimpleImageEditor(item, profile, null);
    ((SuggestBean) BeanHelper.getSessionBean(SuggestBean.class)).init(profile);
    metadataList = new ArrayList<SuperMetadataBean>();
    metadataList.addAll(editor.getItems().get(0).getMds().getTree().getList());
  }

  /**
   * Save the {@link Item} with its {@link Metadata}
   * 
   * @return
   * @throws Exception
   */
  public void save() throws Exception {
    HistorySession hs = (HistorySession) BeanHelper.getSessionBean(HistorySession.class);
    if (editor.save()) {
      FacesContext.getCurrentInstance().getExternalContext().redirect(hs.getCurrentPage().getUrl());
    } else {
      FacesContext.getCurrentInstance().getExternalContext()
          .redirect(hs.getCurrentPage().getCompleteUrl());
    }
  }

  /**
   * Cancel the editing, and reset original values
   * 
   * @return
   * @throws Exception
   */
  public String cancel() throws Exception {
    this.toggleState = "displayMd";
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    Locks.unLock(new Lock(item.getId().toString(), sb.getUser().getEmail()));
    reloadImage();
    editor = new SimpleImageEditor(item, profile, null);
    return "";
  }

  /**
   * Reload the current image
   */
  private void reloadImage() {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    ItemController itemController = new ItemController();
    try {
      item = itemController.retrieve(item.getId(), sessionBean.getUser());
    } catch (Exception e) {
      BeanHelper.error("Error reload item" + e.getMessage());
    }
  }

  /**
   * Show the metadata editor
   * 
   * @return
   */
  public String showEditor() {
    SessionBean sb = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (AuthUtil.staticAuth().updateContent(sb.getUser(), item)) {
      this.toggleState = "editMd";
      try {
        Locks.lock(new Lock(item.getId().toString(), sb.getUser().getEmail()));
      } catch (Exception e) {
        BeanHelper.error(sb.getMessage("error_editor_image_locked"));
      }
    } else {
      BeanHelper.error(sb.getMessage("error_editor_not_allowed"));
    }
    return "";
  }

  public SimpleImageEditor getEditor() {
    return editor;
  }

  public void setEditor(SimpleImageEditor editor) {
    this.editor = editor;
  }

  public Item getImage() {
    return item;
  }

  public void setImage(Item item) {
    this.item = item;
  }

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  public int getMdPosition() {
    return mdPosition;
  }

  public void setMdPosition(int mdPosition) {
    this.mdPosition = mdPosition;
  }

  public String getToggleState() {
    return toggleState;
  }

  public void setToggleState(String toggleState) {
    this.toggleState = toggleState;
  }

  public List<SuperMetadataBean> getMetadataList() {
    return metadataList;
  }

  public void setMetadataList(List<SuperMetadataBean> metadataList) {
    this.metadataList = metadataList;
  }

}
