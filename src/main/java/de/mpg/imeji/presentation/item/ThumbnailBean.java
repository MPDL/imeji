/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.item;

import java.io.Serializable;
import java.net.URI;

import javax.faces.event.ValueChangeEvent;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.controller.business.ItemBusinessController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.vo.Item;
import de.mpg.imeji.logic.vo.MetadataProfile;
import de.mpg.imeji.logic.vo.MetadataSet;
import de.mpg.imeji.logic.vo.Properties.Status;
import de.mpg.imeji.logic.vo.Statement;
import de.mpg.imeji.logic.vo.predefinedMetadata.Link;
import de.mpg.imeji.logic.vo.predefinedMetadata.Metadata;
import de.mpg.imeji.logic.vo.predefinedMetadata.Publication;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.metadata.MetadataSetWrapper;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.session.SessionObjectsController;
import de.mpg.imeji.presentation.util.CommonUtils;
import de.mpg.imeji.util.DateHelper;

/**
 * Bean for Thumbnail list elements. Each element of a list with thumbnail is an instance of a
 * {@link ThumbnailBean}
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ThumbnailBean implements Serializable {
  private static final long serialVersionUID = -8084039496592141508L;
  private String link = "";
  private String filename = "";
  private String caption = "";
  private URI uri = null;
  private String id;
  private boolean selected = false;
  private boolean isInActiveAlbum = false;
  private MetadataSetWrapper mds;
  private MetadataProfile profile;
  private MetadataSet mdSet;
  private URI collectionUri;
  private String fileType;
  private String shortFileType;
  private String fileSize;
  private String modified;

  /**
   * Emtpy {@link ThumbnailBean}
   */
  public ThumbnailBean() {}

  /**
   * Bean for Thumbnail list elements. Each element of a list with thumbnail is an instance of a
   * {@link ThumbnailBean}
   *
   * @param item
   * @param initMetadata if true, will read the metadata
   * @throws Exception
   */
  public ThumbnailBean(Item item, boolean initMetadata) throws Exception {
    this.uri = item.getId();
    this.collectionUri = item.getCollection();
    this.id = ObjectHelper.getId(getUri());
    this.link = initThumbnailLink(item);
    this.filename = item.getFilename();
    this.fileType = item.getFiletype();
    this.fileSize = item.getFileSizeHumanReadable();
    this.modified = DateHelper.printDate(item.getModified());
    this.shortFileType = StorageUtils.getExtension(fileType);
    if (initMetadata) {
      SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
      this.mdSet = item.getMetadataSet();
      this.profile = sessionBean.loadProfileWithoutPrivs(this.mdSet.getProfile());
      this.caption = findCaption();
      this.selected = sessionBean.getSelected().contains(uri.toString());
      if (sessionBean.getActiveAlbum() != null) {
        this.isInActiveAlbum = sessionBean.getActiveAlbum().getImages().contains(item.getId());
      }
    }
  }


  /**
   * Initialize the {@link MetadataSetWrapper} which is used in the Popup
   *
   * @throws ImejiException
   */
  public void initPopup() throws ImejiException {
    SessionBean sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    if (getMds() == null) {
      ItemBusinessController controller = new ItemBusinessController();
      mdSet = controller.retrieve(uri, sessionBean.getUser()).getMetadataSet();
      setMds(new MetadataSetWrapper(mdSet, getProfile(), false));
    }
  }

  /**
   * Find the link (url) to the Thumbnail
   *
   * @param item
   * @return
   */
  private String initThumbnailLink(Item item) {
    Navigation navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    return Status.WITHDRAWN != item.getStatus()
        ? navigation.getFileUrl() + item.getThumbnailImageUrl().toString()
        : navigation.getApplicationUrl() + "resources/icon/discarded.png";
  }


  /**
   * Find the caption for this {@link ThumbnailBean} as defined in the {@link MetadataProfile}. If
   * none defined in the {@link MetadataProfile} return the filename
   *
   * @return
   * @throws ImejiException
   */
  private String findCaption() throws ImejiException {
    if (profile == null) {
      return getFilename();
    }
    for (Statement s : profile.getStatements()) {
      if (s.isDescription()) {
        initPopup();
        for (Metadata md : mdSet.getMetadata()) {
          if (md.getStatement().equals(s.getId())) {
            String str = "";
            if (md instanceof Link) {
              str = ((Link) md).getLabel();
            } else if (md instanceof Publication) {
              str = CommonUtils.removeTags(((Publication) md).getCitation());
            } else {
              str = md.asFulltext();
            }
            if (!"".equals(str.trim())) {
              return str;
            }
          }
        }
      }
    }
    return getFilename();
  }

  /**
   * Listener for the select box of this {@link ThumbnailBean}
   *
   * @param event
   */
  public void selectedChanged(ValueChangeEvent event) {
    SessionObjectsController soc = new SessionObjectsController();
    if (event.getNewValue().toString().equals("true")) {
      setSelected(true);
      soc.selectItem(getUri().toString());
    } else if (event.getNewValue().toString().equals("false")) {
      setSelected(false);
      soc.unselectItem(getUri().toString());
    }
  }

  /**
   * getter
   *
   * @return
   */
  public String getLink() {
    return link;
  }

  /**
   * setter
   *
   * @param link
   */
  public void setLink(String link) {
    this.link = link;
  }

  /**
   * getter
   *
   * @return
   */
  public String getFilename() {
    return filename;
  }

  /**
   * setter
   *
   * @param filename
   */
  public void setFilename(String filename) {
    this.filename = filename;
  }

  /**
   * getter
   *
   * @return
   */
  public String getCaption() {
    return caption;
  }

  /**
   * setter
   *
   * @param caption
   */
  public void setCaption(String caption) {
    this.caption = caption;
  }

  /**
   * getter
   *
   * @return
   */
  public URI getUri() {
    return uri;
  }

  /**
   * setter
   *
   * @param id
   */
  public void setUri(URI id) {
    this.uri = id;
  }

  /**
   * getter
   *
   * @return
   */
  public boolean isSelected() {
    return selected;
  }

  /**
   * setter
   *
   * @param selected
   */
  public void setSelected(boolean selected) {
    this.selected = selected;
  }

  /**
   * getter
   *
   * @return
   */
  public boolean isInActiveAlbum() {
    return isInActiveAlbum;
  }

  /**
   * setter
   *
   * @param isInActiveAlbum
   */
  public void setInActiveAlbum(boolean isInActiveAlbum) {
    this.isInActiveAlbum = isInActiveAlbum;
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
   * @param id
   */
  public void setId(String id) {
    this.id = id;
  }

  /**
   * getter
   *
   * @return the mds
   */
  public MetadataSetWrapper getMds() {
    return mds;
  }

  /**
   * setter
   *
   * @param mds the mds to set
   */
  public void setMds(MetadataSetWrapper mds) {
    this.mds = mds;
  }

  public URI getCollectionUri() {
    return collectionUri;
  }

  public void setCollectionUri(URI colUri) {
    this.collectionUri = colUri;
  }

  public MetadataProfile getProfile() {
    return profile;
  }

  public void setProfile(MetadataProfile profile) {
    this.profile = profile;
  }

  public String getFileType() {
    return fileType;
  }

  public void setFileType(String fileType) {
    this.fileType = fileType;
  }

  public String getFileSize() {
    return fileSize;
  }

  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  public String getModified() {
    return modified;
  }

  public void setModified(String modified) {
    this.modified = modified;
  }

  public String getShortFileType() {
    return shortFileType;
  }

  public void setShortFileType(String shortFileType) {
    this.shortFileType = shortFileType;
  }

}
