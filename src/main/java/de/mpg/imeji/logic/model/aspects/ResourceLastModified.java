package de.mpg.imeji.logic.model.aspects;

import java.util.Calendar;

/**
 * Implement this interface for every data object in store that might be subjected to shared
 * read/write access by multiple processes (session users).
 * 
 * Data synchronization mechanism is as follows: Each object that is read from or written to store
 * has a time stamp that indicates the date of last modification of the object. If a process is to
 * write a data object to store, it will check first, whether the current object is "younger" or
 * "older" than the one that is saved in store. In case the current object has a time stamp with a
 * date older than the one in store, another process has modified the stored object since is was
 * last read by our process.
 * 
 * The data object must then be updated with it's latest version and changes can only be added to
 * this updated version.
 * 
 * @author breddin
 *
 */
public interface ResourceLastModified {

  /**
   * Setter for time stamp field that indicates when a data object was last changed. Field is also
   * written to database. Records system time of last modification of a data object.
   * 
   * @param calendar
   */
  public void setModified(Calendar calendar);

  /**
   * Getter for time stamp field that indicates when a data object was last changed. Field is also
   * written to database.
   * 
   * @return
   */
  public Calendar getModified();


  /**
   * Setter for a field that holds version information of a data object, by means of the time stamp
   * of last modification of the data object. Whenever an object is read from database, last date of
   * modification stored in database is saved here.
   */
  public void setLastTimeStampReadFromDatabase(Calendar calendar);

  /**
   * Getter for a field that holds version information of a data object, by means of the time stamp
   * of last modification of the data object.
   * 
   * @return
   */
  public Calendar getLastTimeStampReadFromDatabase();


}
