/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.presentation.space;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.faces.context.FacesContext;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.TypeNotAllowedException;
import de.mpg.imeji.logic.Imeji;
import de.mpg.imeji.logic.controller.resource.CollectionController;
import de.mpg.imeji.logic.controller.resource.SpaceController;
import de.mpg.imeji.logic.search.SearchQueryParser;
import de.mpg.imeji.logic.storage.StorageController;
import de.mpg.imeji.logic.storage.util.StorageUtils;
import de.mpg.imeji.logic.util.StringHelper;
import de.mpg.imeji.logic.util.TempFileUtil;
import de.mpg.imeji.logic.util.UrlHelper;
import de.mpg.imeji.logic.vo.CollectionImeji;
import de.mpg.imeji.logic.vo.Space;
import de.mpg.imeji.logic.vo.User;
import de.mpg.imeji.presentation.beans.Navigation;
import de.mpg.imeji.presentation.session.BeanHelper;
import de.mpg.imeji.presentation.session.SessionBean;
import de.mpg.imeji.presentation.upload.IngestImage;

/**
 * Abstract bean for all collection beans
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public abstract class SpaceBean implements Serializable {

  private static final long serialVersionUID = -3164518799136292492L;

  private static final Logger LOGGER = Logger.getLogger(SpaceBean.class);

  protected SessionBean sessionBean;
  protected Navigation navigation;
  private Space space;
  private List<CollectionImeji> collections;
  private List<String> selectedCollections = new ArrayList<String>();
  private IngestImage ingestImage;

  private boolean spaceCreateMode = true;
  private boolean backToAdminNoSpace = false;

  /**
   * New default {@link SpaceBean}
   */
  public SpaceBean() {
    space = new Space();
    sessionBean = (SessionBean) BeanHelper.getSessionBean(SessionBean.class);
    navigation = (Navigation) BeanHelper.getApplicationBean(Navigation.class);
    collections = new ArrayList<CollectionImeji>();
  }

  /**
   * @return the space
   */
  public Space getSpace() {
    return space;
  }

  /**
   * @param collection the collection to set
   */
  public void setSpace(Space space) {
    this.space = space;
  }



  /**
   * Delete the {@link CollectionImeji}
   *
   * @return
   */
  public String delete() {
    SpaceController sc = new SpaceController();
    try {
      sc.delete(space, sessionBean.getUser());
      BeanHelper.info(
          Imeji.RESOURCE_BUNDLE.getMessage("space_successfully_deleted", sessionBean.getLocale()));
    } catch (Exception e) {
      BeanHelper
          .error(Imeji.RESOURCE_BUNDLE.getMessage("error_space_delete", sessionBean.getLocale()));
      LOGGER.error("Error delete space", e);
    }

    return "";
  }

  public boolean isSpaceCreateMode() {
    return spaceCreateMode;
  }

  public void setSpaceCreateMode(boolean spaceCreateMode) {
    this.spaceCreateMode = spaceCreateMode;
  }

  public void init() {
    CollectionController cc = new CollectionController();
    String q = "";
    User user = sessionBean.getUser();
    try {
      if (!isSpaceCreateMode()) {

        SpaceController sc = new SpaceController();
        space = sc.retrieve(sessionBean.getSelectedSpace(), sessionBean.getUser());
        backToAdminNoSpace = UrlHelper.getParameterBoolean("admin");
      }


      if (!StringHelper.isNullOrEmptyTrim(space.getIdString())) {
        collections = cc.searchAndRetrieve(SearchQueryParser.parseStringQuery(q), null, user,
            space.getId().toString(), 0, -1);
        for (CollectionImeji selC : collections) {
          selectedCollections.add(selC.getId().toString());
        }
      }

      collections.addAll(cc.retrieveCollectionsNotInSpace(user));

      Collections.sort(collections, new Comparator<CollectionImeji>() {
        @Override
        public int compare(CollectionImeji coll1, CollectionImeji coll2) {
          return coll1.getMetadata().getTitle().compareToIgnoreCase(coll2.getMetadata().getTitle());
        }
      });
    } catch (ImejiException e) {
      BeanHelper.info(Imeji.RESOURCE_BUNDLE.getMessage("could_not_load_collections_for_space",
          sessionBean.getLocale()));
      LOGGER.error("error reading collection spaces", e);
    }
    if (UrlHelper.getParameterBoolean("start")) {
      try {
        upload();
      } catch (FileUploadException e) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_uri_save",
            sessionBean.getLocale()));
        LOGGER.error("error upload space logo", e);
      } catch (TypeNotAllowedException e) {
        BeanHelper.error(Imeji.RESOURCE_BUNDLE.getMessage("error_collection_logo_uri_save",
            sessionBean.getLocale()));
        LOGGER.error("error upload space logo", e);
      }
    }

  }

  public List<SelectItem> getCollectionItems() {
    List<SelectItem> itemList = new ArrayList<SelectItem>();
    for (CollectionImeji ci : collections) {
      itemList.add(new SelectItem(ci.getId().toString(), ci.getMetadata().getTitle()));
    }
    return itemList;
  }

  public String[] getSelectedCollectionItemsArray() {
    int i = 0;
    String[] selItemArray = new String[selectedCollections.size()];
    for (CollectionImeji ci : collections) {
      for (String sel : selectedCollections) {
        if (sel.equals(ci.getId().toString())) {
          selItemArray[i] = ci.getIdString();
          i++;
        }
      }
    }
    return selItemArray;
  }

  public void setSelectedCollectionItemsArray() {
    // do-Nothing
  }

  public List<CollectionImeji> getCollections() {
    return collections;
  }

  public void setCollections(List<CollectionImeji> collections) {
    this.collections = collections;
  }

  public SessionBean getSessionBean() {
    return sessionBean;
  }

  public void setSessionBean(SessionBean sessionBean) {
    this.sessionBean = sessionBean;
  }

  public List<String> getSelectedCollections() {
    return selectedCollections;
  }

  public String[] getSelectedCollectionsArray() {
    return selectedCollections.toArray(new String[selectedCollections.size()]);
  }

  public void setSelectedCollections(List<String> selectedCollections) {
    this.selectedCollections = selectedCollections;
  }

  public void upload() throws FileUploadException, TypeNotAllowedException {
    HttpServletRequest request =
        (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
    HttpServletResponse response =
        (HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse();
    setIngestImage(getUploadedIngestFile(request, response));
  }

  private IngestImage getUploadedIngestFile(HttpServletRequest request,
      HttpServletResponse response) throws FileUploadException, TypeNotAllowedException {
    File tmp = null;
    boolean isMultipart = ServletFileUpload.isMultipartContent(request);
    IngestImage ii = new IngestImage();
    if (isMultipart) {
      ServletFileUpload upload = new ServletFileUpload();
      try {
        FileItemIterator iter = upload.getItemIterator(request);

        while (iter.hasNext()) {
          FileItemStream fis = iter.next();
          InputStream in = fis.openStream();

          tmp = TempFileUtil.createTempFile("spacelogo",
              "." + FilenameUtils.getExtension(fis.getName()));
          if (fis.getName() != null && extensionNotAllowed(tmp)) {
            response.resetBuffer();
            response.getWriter().print(
                "{\"jsonrpc\" : \"2.0\", \"error\" : {\"code\": 400, \"message\": \"Bad Filetype\"}, \"details\" : \"Error description\"}STOP");
            response.flushBuffer();
            throw new TypeNotAllowedException(Imeji.RESOURCE_BUNDLE
                .getMessage("Logo_single_upload_invalid_content_format", sessionBean.getLocale()));
          }
          FileOutputStream fos = new FileOutputStream(tmp);
          if (fis.getName() != null) {
            ii.setName(fis.getName());
          }
          if (!fis.isFormField()) {
            try {
              IOUtils.copy(in, fos);
            } catch (Exception e) {
              BeanHelper.error("Could not process uploaded Logo file streams");
            }

          }
          in.close();
          fos.close();
        }
        ii.setFile(tmp);

      } catch (IOException | FileUploadException e) {
        ii.setFile(null);
        BeanHelper.error("Could not process uploaded Logo file");
        LOGGER.error("Could not process uploaded Logo file", e);
      }
    }
    return ii;
  }

  public boolean extensionNotAllowed(File file) {
    StorageController sc = new StorageController();
    String guessedNotAllowedFormat = sc.guessNotAllowedFormat(file);
    return StorageUtils.BAD_FORMAT.equals(guessedNotAllowedFormat);
  }

  public void setIngestImage(IngestImage im) {
    this.ingestImage = im;
    sessionBean.setSpaceLogoIngestImage(im);
  }

  public IngestImage getIngestImage() {
    return this.ingestImage;
  }

  /**
   * @return the backToAdminNoSpace
   */
  public boolean isBackToAdminNoSpace() {
    return backToAdminNoSpace;
  }


}
