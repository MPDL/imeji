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
package de.mpg.imeji.logic.storage.impl;

import java.io.File;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.util.ProxyHelper;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.UploadResult;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.storage.util.StorageUtils;

/**
 * The {@link Storage} implementation for external Storages. Can only read files (if the files are
 * publicly available).
 *
 * @author saquet (initial creation)
 * @author $Author$ (last modification)
 * @version $Revision$ $LastChangedDate$
 */
public class ExternalStorage implements Storage {
  private static final long serialVersionUID = -5808761436385828641L;
  private HttpClient client;

  /**
   * Default constructor
   */
  public ExternalStorage() {
    client = StorageUtils.getHttpClient();
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getName()
   */
  @Override
  public String getName() {
    return "external";
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#upload(byte[])
   */
  @Override
  public UploadResult upload(String filename, File file, String collectionId) {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#read(java.lang.String)
   */
  @Override
  public void read(String url, OutputStream out, boolean close) throws ImejiException {

    GetMethod get = StorageUtils.newGetMethod(client, url);
    get.setFollowRedirects(true);
    try {
      // client.executeMethod(get);
      ProxyHelper.executeMethod(client, get);
      if (get.getStatusCode() == 302) {
        // Login in escidoc is not valid anymore, log in again an read again
        get.releaseConnection();
        get = StorageUtils.newGetMethod(client, url);
        // client.executeMethod(get);
        ProxyHelper.executeMethod(client, get);
      }
      StorageUtils.writeInOut(get.getResponseBodyAsStream(), out, close);
    } catch (Exception e) {
      // throw new RuntimeException("Error reading " + url, e);
      throw new UnprocessableError("Error reading " + url + " (" + e.getMessage() + ")", e);
    } finally {
      get.releaseConnection();
    }
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#delete(java.lang.String)
   */
  @Override
  public void delete(String url) {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#update(java.lang.String, byte[])
   */
  @Override
  public void update(String url, File file) {
    // TODO Auto-generated method stub
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getAdminstrator()
   */
  @Override
  public StorageAdministrator getAdministrator() {
    // TODO Auto-generated method stub
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getCollectionId(java.lang.String)
   */
  @Override
  public String getCollectionId(String url) {
    // TODO Auto-generated method stub
    return null;
  }


  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#readFileStringContent(java.lang.String)
   */
  @Override
  public String readFileStringContent(String url) {
    return null;
  }

  @Override
  public String getStorageId(String url) {
    return url;
  }
}
