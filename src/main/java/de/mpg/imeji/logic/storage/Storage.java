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
package de.mpg.imeji.logic.storage;

import java.io.File;
import java.io.OutputStream;
import java.io.Serializable;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.vo.CollectionImeji;

/**
 * Interface for imeji storage
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public interface Storage extends Serializable {
  /**
   * The possible resolution of a file in imeji
   *
   * @author saquet (initial creation)
   * @author $Author$ (last modification)
   * @version $Revision$ $LastChangedDate$
   */
  public enum FileResolution {
    THUMBNAIL, WEB, ORIGINAL;
  }

  /**
   * The name (as {@link String}) of the {@link Storage} as defined in the imeji.properties
   *
   * @return
   */
  public String getName();

  /**
   * Upload a file as {@link Byte} array in the {@link Storage}
   *
   * @param file
   * @return - the url of the uploaded File
   */
  public UploadResult upload(String filename, File file, String collectionId);


  /**
   * Read the file stored in the passed url
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public void read(String url, OutputStream out, boolean close) throws ImejiException;

  /**
   * Read the file stored in the passed url
   *
   * @param url
   * @param out
   * @throws ImejiException
   */
  public File read(String url) throws ImejiException;

  /**
   * Delete the file stored in the passed url
   *
   * @param url
   */
  public void delete(String url);

  /**
   * Update the file stored in the passed url with the passed {@link Byte} array
   *
   * @param url
   * @param bytes
   */
  public void update(String url, File file);

  /**
   * Return a {@link StorageAdministrator} for this {@link Storage}
   *
   * @return
   */
  public StorageAdministrator getAdministrator();

  /**
   * Return the id of the {@link CollectionImeji} related to this file
   *
   * @param url
   * @return
   */
  public String getCollectionId(String url);

  /**
   * Read the file stored in the passed url as string
   *
   * @param url
   * @return
   */
  public String readFileStringContent(String url);

  /**
   * Return the Storage id
   *
   * @param url
   * @return
   */
  public String getStorageId(String url);

}
