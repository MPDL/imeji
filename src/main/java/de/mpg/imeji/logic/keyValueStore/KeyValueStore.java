package de.mpg.imeji.logic.keyValueStore;

import java.io.IOException;
import java.util.List;

/**
 * Interface for Key/Value Store
 *
 * @author bastiens
 *
 */
public interface KeyValueStore {

  /**
   * The name of the store
   *
   * @return
   */
  public String getName();

  /**
   * Get the value for the specified key
   *
   * @param b
   * @return
   */
  public byte[] get(String key);

  /**
   * Put a key/value to the store
   *
   * @param key
   * @param value
   * @return
   */
  public void put(String key, byte[] value);

  /**
   * Delete an entry from the store
   *
   * @param key
   */
  public void delete(String key);

  /**
   * Return a list of value which the key matches the pattern
   *
   * @param keyPattern
   * @param clazz
   * @return
   */
  public List<byte[]> getList(String keyPattern);

  /**
   * Start the Key/Value Store
   *
   * @throws IOException
   */
  public void start();

  /**
   * Close the Key/value Store
   *
   * @throws IOException
   */
  public void stop();

  /**
   * True if the Stored has been already started
   *
   * @return
   */
  public boolean isStarted();

  /**
   * Reset all data of store
   */
  public void reset();


}
