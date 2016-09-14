package de.mpg.imeji.presentation.album;


import java.io.File;
import java.io.Serializable;

import javax.annotation.PostConstruct;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.AlbumController;
import de.mpg.imeji.logic.util.ObjectHelper;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.Album;
import de.mpg.imeji.presentation.beans.ContainerEditorSession;
import de.mpg.imeji.presentation.session.BeanHelper;

/**
 * Bean for the edit album page
 *
 * @author bastiens
 *
 */
@ManagedBean(name = "EditAlbumBean")
@ViewScoped
public class EditAlbumBean extends AlbumBean implements Serializable {
  private static final long serialVersionUID = 492235953313907645L;
  public static final Logger LOGGER = Logger.getLogger(EditAlbumBean.class);
  @ManagedProperty(value = "#{ContainerEditorSession}")
  private ContainerEditorSession containerEditorSession;



  @Override
  @PostConstruct
  public void init() {
    try {
      setId(UrlHelper.getParameterValue("id"));
      setAlbum(new AlbumController().retrieve(ObjectHelper.getURI(Album.class, getId()),
          getSessionUser()));
    } catch (Exception e) {
      BeanHelper.error(e.getMessage());
      LOGGER.error("Error init album edit", e);
    }
    containerEditorSession.setUploadedLogoPath(null);
  }

  /**
   * Save (create or update) the {@link Album} in the database
   *
   * @return
   * @throws Exception
   */
  public void save() throws Exception {
    if (update()) {
      redirect(getPageUrl());
    }
  }

  /**
   * Update the {@link Album} in the dabatase with the values defined in this {@link AlbumBean}
   *
   * @return
   * @throws Exception
   */
  public boolean update() throws Exception {
    AlbumController ac = new AlbumController();
    try {
      Album album = ac.update(getAlbum(), getSessionUser());
      if (containerEditorSession.getUploadedLogoPath() != null) {
        ac.updateLogo(album, new File(containerEditorSession.getUploadedLogoPath()),
            getSessionUser());
      }
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("success_album_update", getLocale()));
      return true;
    } catch (UnprocessableError e) {
      BeanHelper.error(e, getLocale());
      LOGGER.error("Error update album", e);
      return false;
    }

  }

  /**
   * @return the containerEditorSession
   */
  public ContainerEditorSession getContainerEditorSession() {
    return containerEditorSession;
  }

  /**
   * @param containerEditorSession the containerEditorSession to set
   */
  public void setContainerEditorSession(ContainerEditorSession containerEditorSession) {
    this.containerEditorSession = containerEditorSession;
  }
}
