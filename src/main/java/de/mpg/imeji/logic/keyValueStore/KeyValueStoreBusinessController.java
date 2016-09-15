package de.mpg.imeji.logic.keyValueStore;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import de.mpg.imeji.exceptions.ImejiException;
import de.mpg.imeji.exceptions.NotFoundException;

/**
 * Business Controller to persisted messages. This can be used to store Notifications, Invitations,
 * etc. Messages are stored in a Key/Value store
 *
 * @author bastiens
 *
 */
public class KeyValueStoreBusinessController {
  private static final Logger LOGGER = Logger.getLogger(KeyValueStoreBusinessController.class);
  private KeyValueStore store;
  private static final Map<String, KeyValueStore> stores = new HashMap<>();

  public KeyValueStoreBusinessController(KeyValueStore store) {
    this.store = store;
    if (!store.isStarted()) {
      store.start();
    }
    stores.put(store.getName(), store);
  }

  /**
   * Start all stores which have been registed
   */
  public synchronized static void startAllStores() {
    for (KeyValueStore kvs : stores.values()) {
      kvs.start();
    }
  }

  /**
   * Stop all started key/values stores
   *
   * @throws IOException
   */
  public synchronized static void stopAllStores() {
    for (KeyValueStore kvs : stores.values()) {
      LOGGER.info("Stopping store: " + kvs.getName());
      kvs.stop();
    }
  }

  /**
   * Stop all started key/values stores
   *
   * @throws IOException
   */
  public synchronized static void resetAllStores() {
    for (KeyValueStore kvs : stores.values()) {
      LOGGER.info("Resetting store: " + kvs.getName());
      kvs.reset();
    }
  }

  /**
   * Resetting and stopping all stores
   */
  public synchronized static void resetAndStopAllStores() {
    for (KeyValueStore kvs : stores.values()) {
      LOGGER.info("Resetting and stopping store: " + kvs.getName());
      kvs.reset();
      kvs.stop();
    }
  }

  /**
   * Get an Object from the Key/Value Store
   *
   * @param id
   * @return
   * @throws ImejiException
   */
  public Object get(String key) throws NotFoundException {
    try {
      return deserialize(store.get(key));
    } catch (Exception e) {
      throw new NotFoundException(
          "Key " + key + " not found in  " + store.getName() + ": " + e.getMessage());
    }
  }

  /**
   * Return all elements with a Key matching the Key pattern (according to REGEX Rules)
   *
   * @param keyPattern
   * @return
   * @throws ImejiException
   * @throws ClassNotFoundException
   * @throws IOException
   */

  public <T> List<T> getList(String keyPattern, Class<T> clazz) throws ImejiException {
    List<T> list = new ArrayList<>();
    for (byte[] b : store.getList(keyPattern)) {
      try {
        list.add(clazz.cast(deserialize(b)));
      } catch (ClassNotFoundException | IOException e) {
        throw new ImejiException("Error reading date from " + store.getName(), e);
      }
    }
    return list;
  }

  /**
   * Put an object to the Key/Value Store
   *
   * @param id
   * @param o
   * @throws ImejiException
   */
  public void put(String key, Object value) throws ImejiException {
    try {
      store.put(key, serialize(value));
    } catch (Exception e) {
      throw new ImejiException("Error writing Data in Key/Value Store", e);
    }
  }

  /**
   * Delete the key/value by its key
   *
   * @param key
   * @throws ImejiException
   */
  public void delete(String key) throws ImejiException {
    try {
      store.delete(key);
    } catch (Exception e) {
      throw new ImejiException("Error deleting Data " + store.getName(), e);
    }
  }

  /**
   * Create the Object from its serialized byte representation
   *
   * @param bytes
   * @return
   * @throws IOException
   * @throws ClassNotFoundException
   */
  private Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
    ByteArrayInputStream in = new ByteArrayInputStream(bytes);
    ObjectInputStream is = new ObjectInputStream(in);
    return is.readObject();
  }

  /**
   * Serialize an Object to a byte array
   *
   * @param obj
   * @return
   * @throws IOException
   */
  private byte[] serialize(Object obj) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ObjectOutputStream os = new ObjectOutputStream(out);
    os.writeObject(obj);
    return out.toByteArray();
  }

}
