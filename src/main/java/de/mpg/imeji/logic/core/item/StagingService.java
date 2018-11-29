package de.mpg.imeji.logic.core.item;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.UnprocessableError;
import de.mpg.imeji.logic.model.Item;
import de.mpg.imeji.logic.model.UploadResult;
import de.mpg.imeji.logic.model.User;
import de.mpg.imeji.logic.security.user.util.QuotaUtil;
import de.mpg.imeji.logic.storage.StorageController;

/**
 * Staging service to upload data in a staging area
 * 
 * @author saquet
 *
 */
class StagingService implements Serializable {
  private static final long serialVersionUID = -8577623775722783511L;
  private static HashMap<String, List<StagingFile>> stagingFiles = new HashMap<>();

  class StagingFile implements Serializable {
    private static final long serialVersionUID = -8876563324381426735L;
    private final Item item;
    private final UploadResult uploadResult;
    private final long quota;

    public StagingFile(Item item, UploadResult uploadResult, long quota) {
      this.item = item;
      this.uploadResult = uploadResult;
      this.quota = quota;
    }

    /**
     * @return the item
     */
    public Item getItem() {
      return item;
    }

    /**
     * @return the uploadResult
     */
    public UploadResult getUploadResult() {
      return uploadResult;
    }

    /**
     * @return the quota
     */
    public long getQuota() {
      return quota;
    }

  }

  public StagingService() {

  }

  /**
   * Add a File to the staging area
   * 
   * @param uploadId
   * @param filename
   * @param file
   * @throws ImejiException
   */
  void add(String uploadId, Item item, File file, long quota) throws ImejiException {
    if (!stagingFiles.containsKey(uploadId)) {
      stagingFiles.put(uploadId, new ArrayList<>());
    }
    UploadResult uploadResult = new StorageController().upload(item.getFilename(), file);
    stagingFiles.get(uploadId).add(new StagingFile(item, uploadResult, quota + file.length()));
  }

  /**
   * Retrieve all files from the staging area for this upload and remove then from the staging area
   * 
   * @param uploadId
   * @return
   */
  List<StagingFile> retrieveAndRemove(String uploadId) {
    List<StagingFile> l = stagingFiles.get(uploadId);
    stagingFiles.remove(uploadId);
    return l;
  }

  /**
   * Retrieve all files from the staging area for this upload
   * 
   * @param uploadId
   * @return
   */
  List<StagingFile> retrieve(String uploadId) {
    return stagingFiles.get(uploadId);
  }

  /**
   * Throw an error if one staged file for this upload has the same checksum
   * 
   * @param uploadId
   * @param checksum
   * @return
   * @throws UnprocessableError
   */
  public void validateChecksum(String uploadId, String checksum) throws UnprocessableError {
    List<StagingFile> l = stagingFiles.get(uploadId);
    if (l != null && l.stream().anyMatch(staging -> staging.getUploadResult().getChecksum().equals(checksum))) {
      throw new UnprocessableError("Same file already exists in the collection (with same checksum). Please choose another file.");
    }
  }

  /**
   * REturn the used quota of the user including staging files
   * 
   * @param uploadId
   * @param user
   */
  public long getUsedQuota(String uploadId, User user) {
    List<StagingFile> stagedFiles = retrieve(uploadId);
    return stagedFiles == null ? QuotaUtil.getUsedQuota(user) : stagedFiles.get(0).getQuota();
  }
}
