package de.mpg.imeji.exceptions;

/**
 * Exception will be thrown if a process (user session) tries to update an object in store that has
 * been changed since it was last read by that process. This can happen when another process (user
 * session) updates the object after it is read from database by our process and before it is
 * written back. Context: Multiple processes access shared data resources (i.e. collection, item).
 * 
 * The latest version of the object in store can be retrieved from the Exception.
 * 
 * @author breddin
 *
 */
public class ReloadBeforeSaveException extends ImejiException {

  private static final long serialVersionUID = -2208211062675780202L;
  private Object latestDataObject;

  public ReloadBeforeSaveException(Object latestDataObject) {
    super("Object has been altered in store. Reload before saving.");
    this.latestDataObject = latestDataObject;
  }

  public Object getLatestDataObject() {
    return this.latestDataObject;
  }

}
