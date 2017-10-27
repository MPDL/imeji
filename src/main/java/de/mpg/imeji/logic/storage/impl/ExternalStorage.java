package de.mpg.imeji.logic.storage.impl;

import java.awt.Dimension;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.methods.GetMethod;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.config.util.ProxyHelper;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.storage.Storage;
import de.mpg.imeji.logic.storage.administrator.StorageAdministrator;
import de.mpg.imeji.logic.util.StorageUtils;
import de.mpg.imeji.logic.util.TempFileUtil;

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
  private final HttpClient client;

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
  public UploadResult upload(String filename, File file) {
    // Not implemented
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
    } catch (final Exception e) {
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
    // Not implemented
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#update(java.lang.String, byte[])
   */
  @Override
  public void changeThumbnail(String url, File file) {
    // Not implemented
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getAdminstrator()
   */
  @Override
  public StorageAdministrator getAdministrator() {
    // Not implemented
    return null;
  }

  /*
   * (non-Javadoc)
   *
   * @see de.mpg.imeji.logic.storage.Storage#getCollectionId(java.lang.String)
   */
  @Override
  public String getCollectionId(String url) {
    // Not implemented
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

  @Override
  public File read(String url) throws ImejiException {
    try {
      final File temp = TempFileUtil.createTempFile(url, null);
      read(url, new FileOutputStream(temp), true);
      return temp;
    } catch (final IOException e) {
      throw new ImejiException("error reading file " + url, e);
    }
  }

  @Override
  public void update(String url, File file) throws IOException {
    // Not implemented

  }

  @Override
  public void rotate(String originalUrl, int degrees) throws ImejiException {
    // Not implemented
  }


  @Override
  public void recalculateWebAndThumbnail(String fullUrl, String webUrl, String thumbnailUrl)
      throws IOException, Exception {
    // TODO Auto-generated method stub

  }

  @Override
  public Dimension getImageDimension(String url) throws IOException {
    // TODO Auto-generated method stub
    return null;
  }

  @Override
  public double getContentLenght(String url) {
    return 0;
  }

  @Override
  public void readPart(String url, OutputStream out, boolean close, long offset, long length)
      throws ImejiException {
    read(url, out, close);
  }
}
