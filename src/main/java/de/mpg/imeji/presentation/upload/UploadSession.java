/*
 *
 * CDDL HEADER START
 *
 * The contents of this file are subject to the terms of the Common Development and Distribution
 * License, Version 1.0 only (the "License"). You may not use this file except in compliance with
 * the License.
 *
 * You can obtain a copy of the license at license/ESCIDOC.LICENSE or http://www.escidoc.de/license.
 * See the License for the specific language governing permissions and limitations under the
 * License.
 *
 * When distributing Covered Code, include this CDDL HEADER in each file and include the License
 * file at license/ESCIDOC.LICENSE. If applicable, add the following below this CDDL HEADER, with
 * the fields enclosed by brackets "[]" replaced with your own identifying information: Portions
 * Copyright [yyyy] [name of copyright owner]
 *
 * CDDL HEADER END
 */
/*
 * Copyright 2006-2007 Fachinformationszentrum Karlsruhe Gesellschaft für
 * wissenschaftlich-technische Information mbH and Max-Planck- Gesellschaft zur Förderung der
 * Wissenschaft e.V. All rights reserved. Use is subject to license terms.
 */
package de.mpg.imeji.presentation.upload;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.apache.commons.lang.BooleanUtils;

import de.mpg.imeji.logic.util.UrlHelper;

/**
 * Session for the upload page for the parameter which must be in a sesion (because of the upload
 * which call the page many times)
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
@ManagedBean(name = "UploadSession")
@SessionScoped
public class UploadSession implements Serializable {
  private static final long serialVersionUID = 8094256535052643070L;
  private List<UploadItem> sFiles;
  private List<String> fFiles;
  private boolean importImageToFile = false;
  private boolean uploadFileToItem = false;
  private boolean checkNameUnique = true;
  private List<UploadItem> itemsToEdit;

  /**
   * DEfault Constructor
   *
   * @throws URISyntaxException
   * @throws IOException
   */
  public UploadSession() throws IOException, URISyntaxException {
    this.sFiles = new ArrayList<UploadItem>();
    this.fFiles = new ArrayList<String>();
  }

  /**
   * Reset to default value
   */
  public void reset() {
    sFiles = new ArrayList<>();
    fFiles = new ArrayList<>();
    itemsToEdit = new ArrayList<>();
    resetProperties();
  }

  public void resetProperties() {
    importImageToFile = false;
    uploadFileToItem = false;
    checkNameUnique = true;
  }

  /**
   * Reset to default value
   */
  public String resetUploads() {
    if (UrlHelper.getParameterBoolean("done")) {
      itemsToEdit.addAll(sFiles);
      sFiles.clear();
      fFiles.clear();
    }
    return "";
  }

  public void uploadFileToItemListener() {
    this.uploadFileToItem = BooleanUtils.negate(uploadFileToItem);
  }

  public void importImageToFileListener() {
    this.importImageToFile = BooleanUtils.negate(importImageToFile);
  }

  public void checkNameUniqueListener() {
    this.checkNameUnique = BooleanUtils.negate(checkNameUnique);
  }

  /**
   * @return the sFiles
   */
  public List<UploadItem> getsFiles() {
    return sFiles;
  }

  /**
   * @param sFiles the sFiles to set
   */
  public void setsFiles(List<UploadItem> sFiles) {
    this.sFiles = sFiles;
  }

  /**
   * @return the fFiles
   */
  public List<String> getfFiles() {
    return fFiles;
  }

  /**
   * @param fFiles the fFiles to set
   */
  public void setfFiles(List<String> fFiles) {
    this.fFiles = fFiles;
  }

  /**
   * @return the importImageToFile
   */
  public boolean isImportImageToFile() {
    return importImageToFile;
  }

  /**
   * @param importImageToFile the importImageToFile to set
   */
  public void setImportImageToFile(boolean importImageToFile) {
    this.importImageToFile = importImageToFile;
  }

  /**
   * @return the uploadFileToItem
   */
  public boolean isUploadFileToItem() {
    return uploadFileToItem;
  }

  /**
   * @param uploadFileToItem the uploadFileToItem to set
   */
  public void setUploadFileToItem(boolean uploadFileToItem) {
    this.uploadFileToItem = uploadFileToItem;
  }

  /**
   * @return the checkNameUnique
   */
  public boolean isCheckNameUnique() {
    return checkNameUnique;
  }

  /**
   * @param checkNameUnique the checkNameUnique to set
   */
  public void setCheckNameUnique(boolean checkNameUnique) {
    this.checkNameUnique = checkNameUnique;
  }

  /**
   * @return the itemsToEdit
   */
  public List<UploadItem> getItemsToEdit() {
    return itemsToEdit;
  }

  public void setItemsToEdit(List<UploadItem> itemsToEdit) {
    this.itemsToEdit = itemsToEdit;
  }

}
