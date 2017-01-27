package de.mpg.imeji.logic.storage;

import java.io.File;
import java.io.IOException;
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
    THUMBNAIL, WEB, ORIGINAL, FULL;
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
   * Copy the file to a new location. All resolution of this file will copy as well
   * 
   * @param storageId
   * @throws IOException
   */
  public UploadResult copy(String url, String collectionId) throws IOException;

  /**
   * Update the file stored in the passed url with a new original resolution (original resolution is
   * not updated)
   *
   * @param url
   * @param bytes
   */
  public void changeThumbnail(String url, File file);


  /**
   * Update the file defined by the url with a new file
   *
   * @param url
   * @param file
   * @throws IOException
   */
  public void update(String url, File file) throws IOException;

  /**
   * Rotate the file
   *
   * @param originalUrl
   * @param degrees
   * @throws ImejiException
   * @throws Exception
   * @throws IOException
   */
  public void rotate(String originalUrl, int degrees) throws ImejiException, IOException, Exception;

  /**
   * Returns the width of the image at the url
   *
   * @param url
   * @return
   */
  public int getImageWidth(String url) throws IOException;

  /**
   * Returns the height of the image at the url
   *
   * @param url
   * @return
   */
  public int getImageHeight(String url) throws IOException;

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

  /**
   * Recreates Web Resolution and Thumbnail in the new sizes
   * 
   * @throws Exception
   * @throws IOException
   * 
   */
  public void recalculateWebAndThumbnail(String fullUrl, String webUrl, String thumbnailUrl)
      throws IOException, Exception;

}
