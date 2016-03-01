/**
 * License: src/main/resources/license/escidoc.license
 */
package de.mpg.imeji.logic.concurrency.locks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.log4j.Logger;

public class Locks {
  // Lock on User Event
  private static Map<String, Lock> userLocks = new ConcurrentHashMap<String, Lock>();
  // Locks on System event
  private static Map<String, Lock> sysLocks = new ConcurrentHashMap<String, Lock>();
  // Life time for a lock. After that limit, the lock is destroyed.
  private static final long LOCK_MAX_TIME = 900000;// 900s = 15min
  // private static final long LOCK_MAX_TIME = 60000;// 60s = 1min
  private static final Logger LOGGER = Logger.getLogger(Locks.class);
  private static boolean writeLock = false;
  private static boolean counterLock = false;

  public static void reset() {
    userLocks = new ConcurrentHashMap<String, Lock>();
    sysLocks = new ConcurrentHashMap<String, Lock>();
  }

  public static Map<String, Lock> getUserLocks() {
    return userLocks;
  }

  public static Map<String, Lock> getSystemLocks() {
    return sysLocks;
  }

  /**
   * return true if the uri (i.e object) is locked for the user. <br/>
   * Check first if uri is locked by system. <br/>
   * Then check if uri is locked by one other user.
   * 
   * @param uri
   * @param email
   * @return
   */
  public static boolean isLocked(String uri, String email) {
    Lock syslock = getSystemLocks().get(uri);
    if (syslock != null) {
      return true;
    } else {
      Lock userLock = getUserLocks().get(uri);
      if (userLock != null && email != null && !userLock.getEmail().equals(email)) {
        return true;
      }
      return false;
    }
  }

  /**
   * If lock doesn't already exist, then add a lock to: <br/>
   * System locks if email is null in Lock <br/>
   * User locks if email is defined.
   * 
   * @param lock
   */
  public static void lock(Lock lock) {
    if (!isLocked(lock.getUri(), lock.getEmail())) {
      if (lock.getEmail() == null) {
        LOGGER.debug(lock.getUri() + " locked by system");
        getSystemLocks().put(lock.getUri(), lock);
      } else {
        LOGGER.debug(lock.getUri() + " locked by " + lock.getEmail());
        getUserLocks().put(lock.getUri(), lock);
      }
    } else {
      throw new RuntimeException(lock.getUri() + " already locked by another user "
          + lock.getEmail());
    }
  }

  /**
   * Unlock the lock.
   * 
   * @param lock
   */
  public static void unLock(Lock lock) {
    if (lock.getEmail() == null && getSystemLocks().get(lock.getUri()) != null) {
      LOGGER.debug(lock.getUri() + " unlocked by system");
      getSystemLocks().remove(lock.getUri());
    } else if (lock.getEmail() != null && getUserLocks().get(lock.getUri()) != null) {
      LOGGER.debug(lock.getUri() + " unlocked by " + lock.getEmail());
      getUserLocks().remove(lock.getUri());
    }
  }

  /**
   * Unlock all locks for one User
   * 
   * @param email
   */
  public static void unlockAll(String email) {
    List<Lock> toUnlock = new ArrayList<Lock>();
    if (!getUserLocks().isEmpty() && email != null) {
      for (Lock l : getUserLocks().values()) {
        if (email.equals(l.getEmail()))
          toUnlock.add(l);
      }
    }
    for (Lock l : toUnlock) {
      unLock(l);
    }
  }

  /**
   * Retreive a list of all locks which are expired
   * 
   * @return
   */
  public static List<Lock> getExpiredLocks() {
    List<Lock> list = new ArrayList<Lock>();
    if (!getUserLocks().isEmpty()) {
      List<Lock> l1 = new ArrayList<Lock>(userLocks.values());
      for (Lock lock : l1) {
        long created = lock.getCreateTime();
        long current = System.currentTimeMillis();
        if (current > (LOCK_MAX_TIME + created)) {
          list.add(lock);
        }
      }
    }
    if (!getSystemLocks().isEmpty()) {
      List<Lock> l1 = new ArrayList<Lock>(sysLocks.values());
      for (Lock lock : l1) {
        long created = lock.getCreateTime();
        long current = System.currentTimeMillis();
        if (current > (LOCK_MAX_TIME + created)) {
          list.add(lock);
        }
      }
    }
    return list;
  }

  public synchronized static boolean tryLock() {
    long startLocked = System.currentTimeMillis();
    while (writeLock) {
      // wait for lock to be released
      if ((System.currentTimeMillis() - startLocked) > 10000) {
        // if a lock is kept more than 10 s, throw exception
        throw new RuntimeException(
            "Write lock could not be released in less than 10s. Check if there is no dead locks");
      }
    }
    lockForWrite();
    return true;
  }

  public static void lockForWrite() {
    writeLock = true;
  }

  public static void releaseLockForWrite() {
    writeLock = false;
  }

  public static void lockCounter() {
    counterLock = true;
  }

  public static void releaseCounter() {
    counterLock = false;
  }

  public synchronized static boolean tryLockCounter() {
    long startLocked = System.currentTimeMillis();
    while (counterLock) {
      // wait for lock to be released
      if ((System.currentTimeMillis() - startLocked) > 10000) {
        // if a lock is kept more than 10 s, throw exception
        throw new RuntimeException(
            "Write lock could not be released in less than 10s. Check if there is no dead locks");
      }
    }
    lockCounter();
    return true;
  }
}
