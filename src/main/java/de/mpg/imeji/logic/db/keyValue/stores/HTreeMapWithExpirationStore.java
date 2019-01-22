package de.mpg.imeji.logic.db.keyValue.stores;

import java.io.File;
import java.util.concurrent.TimeUnit;

import org.mapdb.DBMaker;
import org.mapdb.Serializer;

import de.mpg.imeji.logic.config.Imeji;
import de.mpg.imeji.logic.util.StringHelper;

/**
 * Extends the HTreeMapStore to add expiration capability
 *
 * @author bastiens
 *
 */
public class HTreeMapWithExpirationStore extends HTreeMapStore {
  private long expiration = -1;

  public HTreeMapWithExpirationStore(String name, long expirationInDays) {
    super(name);
    this.expiration = expirationInDays;
  }

  @Override
  public void start() {
    STORE = DBMaker.newFileDB(new File(StringHelper.normalizePath(Imeji.tdbPath) + STORE_FILENAME_PREFIX + name)).make();
    map = STORE.createHashMap(name).keySerializer(Serializer.STRING).expireAfterWrite(this.expiration, TimeUnit.DAYS).makeOrGet();
  }

}
